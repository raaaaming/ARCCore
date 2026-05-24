package cc.arccore.config.runtime

import cc.arccore.config.runtime.async.AsyncConfigDispatchStrategy
import cc.arccore.config.runtime.config.ConfigEntry
import cc.arccore.config.runtime.config.ConfigFormat
import cc.arccore.config.runtime.diagnostics.DefaultConfigDiagnostics
import cc.arccore.config.runtime.exception.ConfigRuntimeException
import cc.arccore.config.runtime.exception.ConfigValidationException
import cc.arccore.config.runtime.integration.DiagnosticsConfigBridgePort
import cc.arccore.config.runtime.integration.FileSystemConfigBridgePort
import cc.arccore.config.runtime.integration.NoopDiagnosticsConfigBridgePort
import cc.arccore.config.runtime.integration.StorageConfigBridgePort
import cc.arccore.config.runtime.ownership.ConfigOwnershipRegistry
import cc.arccore.config.runtime.ownership.DefaultConfigOwnershipRegistry
import cc.arccore.config.runtime.reload.ConfigReloadCoordinator
import cc.arccore.config.runtime.reload.ReloadGeneration
import cc.arccore.config.runtime.reload.ReloadResult
import cc.arccore.config.runtime.serializer.SerializerRegistry
import cc.arccore.config.runtime.serializer.YamlRawReader
import cc.arccore.config.runtime.validation.DefaultValidationPipelineFactory
import cc.arccore.config.runtime.validation.ValidationResult
import cc.arccore.config.runtime.validation.ValidationPipelineFactory
import cc.arccore.config.runtime.watcher.ConfigWatcherRegistry
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * Full implementation of [ConfigRuntime] for a single module.
 *
 * Responsibilities:
 * - Loading typed config from the filesystem (via [storagePort]) or a classpath fallback
 * - Deserialization via [SerializerRegistry] (falls back to [MapConfigSerializer])
 * - Validation via [ValidationPipelineFactory]
 * - Hot reload with generation tracking and handle staleness marking
 * - Ownership tracking so all handles are closed when [unloadAll] is called
 * - Diagnostics notification via [DiagnosticsConfigBridgePort]
 *
 * Thread safety:
 * - [isShutdown] is an [AtomicBoolean]
 * - Cache is a [ConcurrentHashMap]; per-path handle lists use [CopyOnWriteArrayList]
 * - [computeIfAbsent] used for all map initializations
 */
class DefaultConfigRuntime(
    val moduleId: String,
    val baseDataPath: Path,
    val serializerRegistry: SerializerRegistry = SerializerRegistry(),
    val validationPipeline: ValidationPipelineFactory = DefaultValidationPipelineFactory(),
    val ownershipRegistry: ConfigOwnershipRegistry = DefaultConfigOwnershipRegistry(),
    val reloadGeneration: ReloadGeneration = ReloadGeneration(),
    val asyncStrategy: AsyncConfigDispatchStrategy? = null,
    val diagnosticsPort: DiagnosticsConfigBridgePort = NoopDiagnosticsConfigBridgePort,
    val storagePort: StorageConfigBridgePort? = null,
    val watchers: ConfigWatcherRegistry = ConfigWatcherRegistry()
) : ConfigRuntime {

    private val isShutdown = AtomicBoolean(false)

    // Resolved storagePort: use the explicit port if given, else fall back to filesystem I/O
    private val effectiveStoragePort: StorageConfigBridgePort = storagePort
        ?: FileSystemConfigBridgePort(baseDataPath)

    // Cache: path → ConfigEntry<*>
    private val cache: ConcurrentHashMap<String, ConfigEntry<*>> = ConcurrentHashMap()

    // Per-path handle lists for staleness marking on reload
    // path → CopyOnWriteArrayList of DefaultConfigHandle<*>
    private val handlesByPath: ConcurrentHashMap<String, CopyOnWriteArrayList<DefaultConfigHandle<*>>> =
        ConcurrentHashMap()

    private val diagnostics = DefaultConfigDiagnostics()

    private val reloadCoordinator = ConfigReloadCoordinator(
        moduleId = moduleId,
        serializerRegistry = serializerRegistry,
        validationPipeline = validationPipeline,
        reloadGeneration = reloadGeneration,
        storagePort = effectiveStoragePort
    )

    // ─── Load ───────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> load(clazz: KClass<T>, path: String): ConfigHandle<T> {
        checkNotShutdown()

        val startNanos = System.nanoTime()

        // 1. Read raw content
        val rawContent = effectiveStoragePort.readFile(moduleId, path)
            ?: throw ConfigRuntimeException(
                "Config file not found for module '$moduleId' at path '$path'"
            )

        // 2. Parse to Map
        val data = parseRaw(rawContent, ConfigFormat.YAML)

        // 3. Deserialize
        val instance: T = try {
            serializerRegistry.getOrDefault(ConfigFormat.YAML, clazz).deserialize(data, clazz)
        } catch (e: Exception) {
            throw ConfigRuntimeException(
                "Failed to deserialize config '${clazz.simpleName}' from '$path': ${e.message}", e
            )
        }

        // 4. Validate
        val validationResult = validationPipeline.create(clazz).validate(instance)
        if (!validationResult.isValid) {
            val errors = (validationResult as ValidationResult.Invalid).errors
            throw ConfigValidationException(path, errors)
        }

        // 5. Capture generation and create handle
        val generation = reloadGeneration.current()
        val handle = DefaultConfigHandle(
            moduleId = moduleId,
            configPath = path,
            value = instance,
            generation = generation,
            reloadGeneration = reloadGeneration
        )

        // 6. Store in cache
        cache[path] = ConfigEntry(
            moduleId = moduleId,
            path = path,
            value = instance,
            format = ConfigFormat.YAML,
            generation = generation
        )

        // 7. Track handle for staleness marking
        handlesByPath.computeIfAbsent(path) { CopyOnWriteArrayList() }.add(handle)

        // 8. Register ownership so unloadAll() can close it
        ownershipRegistry.register(moduleId, handle)

        // 9. Notify diagnostics
        val durationNanos = System.nanoTime() - startNanos
        diagnostics.recordLoad(moduleId)
        diagnosticsPort.onConfigLoaded(
            moduleId = moduleId,
            path = path,
            clazz = clazz.qualifiedName ?: clazz.simpleName ?: "Unknown",
            durationNanos = durationNanos
        )

        return handle
    }

    // ─── Reload ─────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> reload(handle: ConfigHandle<T>): ReloadResult {
        checkNotShutdown()

        if (handle !is DefaultConfigHandle<T>) {
            return ReloadResult.Failure(
                path = handle.configPath,
                cause = ConfigRuntimeException("Cannot reload a non-DefaultConfigHandle instance")
            )
        }

        // Determine the class from the cached entry
        val cachedEntry = cache[handle.configPath]
            ?: return ReloadResult.NoChange("No cached entry found for '${handle.configPath}'")

        @Suppress("UNCHECKED_CAST")
        val clazz = cachedEntry.value::class as KClass<T>
        val format = cachedEntry.format

        // Collect current handles for this path so we can mark them stale after reload
        @Suppress("UNCHECKED_CAST")
        val existingHandles = (handlesByPath[handle.configPath] ?: CopyOnWriteArrayList())
            .filterIsInstance<DefaultConfigHandle<T>>()

        val result = reloadCoordinator.reload(
            clazz = clazz,
            path = handle.configPath,
            format = format,
            staleHandles = existingHandles
        )

        if (result is ReloadResult.Success) {
            diagnostics.recordReload(moduleId)
            // Remove stale handles from tracking list — they are now closed/stale
            handlesByPath[handle.configPath]?.removeAll(existingHandles.toSet())
        }

        diagnosticsPort.onConfigReloaded(moduleId, handle.configPath, result)
        return result
    }

    // ─── Unload ─────────────────────────────────────────────────────────────

    override fun unloadAll(moduleId: String) {
        checkNotShutdown()

        ownershipRegistry.closeAll(moduleId)
        // Clean up handle tracking for this module's paths
        val pathsToRemove = handlesByPath.keys.filter { path ->
            cache[path]?.moduleId == moduleId
        }
        for (path in pathsToRemove) {
            handlesByPath.remove(path)
            cache.remove(path)
            diagnosticsPort.onConfigUnloaded(moduleId, path)
        }
    }

    // ─── Shutdown ───────────────────────────────────────────────────────────

    override fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            ownershipRegistry.closeAllHandles()
            handlesByPath.clear()
            cache.clear()
            watchers.stopAll()
            diagnosticsPort.onShutdown()
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun checkNotShutdown() {
        if (isShutdown.get()) {
            throw ConfigRuntimeException("ConfigRuntime for module '$moduleId' has been shut down")
        }
    }

    private fun parseRaw(content: String, format: ConfigFormat): Map<String, Any?> {
        return when (format) {
            ConfigFormat.YAML -> YamlRawReader.parse(content)
            ConfigFormat.JSON -> parseJsonFlat(content)
            ConfigFormat.PROPERTIES -> parseProperties(content)
            else -> YamlRawReader.parse(content)
        }
    }

    private fun parseJsonFlat(content: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val trimmed = content.trim().removePrefix("{").removeSuffix("}").trim()
        if (trimmed.isEmpty()) return result
        var i = 0
        var inString = false
        val segments = mutableListOf<String>()
        val sb = StringBuilder()
        while (i < trimmed.length) {
            val ch = trimmed[i]
            when {
                ch == '"' -> { inString = !inString; sb.append(ch) }
                ch == ',' && !inString -> { segments.add(sb.toString().trim()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        if (sb.isNotBlank()) segments.add(sb.toString().trim())
        for (segment in segments) {
            val colonIdx = segment.indexOf(':')
            if (colonIdx < 0) continue
            val key = segment.substring(0, colonIdx).trim().trim('"')
            val rawValue = segment.substring(colonIdx + 1).trim()
            result[key] = when {
                rawValue == "null" -> null
                rawValue == "true" -> true
                rawValue == "false" -> false
                rawValue.startsWith('"') && rawValue.endsWith('"') -> rawValue.substring(1, rawValue.length - 1)
                rawValue.toIntOrNull() != null -> rawValue.toInt()
                rawValue.toLongOrNull() != null -> rawValue.toLong()
                rawValue.toDoubleOrNull() != null -> rawValue.toDouble()
                else -> rawValue
            }
        }
        return result
    }

    private fun parseProperties(content: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) continue
            val eqIdx = trimmed.indexOf('=').takeIf { it >= 0 }
                ?: trimmed.indexOf(':').takeIf { it >= 0 }
                ?: continue
            val key = trimmed.substring(0, eqIdx).trim()
            val value = trimmed.substring(eqIdx + 1).trim()
            result[key] = value
        }
        return result
    }
}

