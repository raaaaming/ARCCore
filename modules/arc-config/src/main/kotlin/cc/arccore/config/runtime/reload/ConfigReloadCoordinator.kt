package cc.arccore.config.runtime.reload

import cc.arccore.config.runtime.DefaultConfigHandle
import cc.arccore.config.runtime.config.ConfigFormat
import cc.arccore.config.runtime.integration.StorageConfigBridgePort
import cc.arccore.config.runtime.serializer.SerializerRegistry
import cc.arccore.config.runtime.serializer.YamlRawReader
import cc.arccore.config.runtime.validation.ValidationPipelineFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Manages the hot reload flow for a single module.
 *
 * Thread safety: concurrent reload requests for the same path are serialized via
 * per-path [synchronized] locks stored in [pathLocks]. Reloads for different paths
 * proceed concurrently.
 *
 * Flow: read → deserialize → validate → if valid: increment generation + mark old handles stale;
 * if invalid: return [ReloadResult.Failure] without touching existing handles.
 */
class ConfigReloadCoordinator(
    private val moduleId: String,
    private val serializerRegistry: SerializerRegistry,
    private val validationPipeline: ValidationPipelineFactory,
    private val reloadGeneration: ReloadGeneration,
    private val storagePort: StorageConfigBridgePort?
) {

    // Per-path lock objects for fine-grained synchronization
    private val pathLocks: ConcurrentHashMap<String, Any> = ConcurrentHashMap()

    /**
     * Reloads the config at [path] for the given [clazz].
     *
     * If successful, marks all [staleHandles] as stale and increments the generation.
     * On failure, existing handles are left intact.
     *
     * @param staleHandles The currently-active handles for this path that should be invalidated.
     * @return [ReloadResult.Success], [ReloadResult.Failure], or [ReloadResult.NoChange].
     */
    fun <T : Any> reload(
        clazz: KClass<T>,
        path: String,
        format: ConfigFormat,
        staleHandles: List<DefaultConfigHandle<T>>
    ): ReloadResult {
        val lock = pathLocks.computeIfAbsent(path) { Any() }
        return synchronized(lock) {
            val startNanos = System.nanoTime()

            // 1. Read raw content
            val rawContent = readContent(path) ?: return@synchronized ReloadResult.NoChange(
                "Config file not found for path '$path' during reload"
            )

            // 2. Deserialize
            val instance: T = try {
                val data = when (format) {
                    cc.arccore.config.runtime.config.ConfigFormat.YAML -> YamlRawReader.parse(rawContent)
                    cc.arccore.config.runtime.config.ConfigFormat.JSON -> parseJson(rawContent)
                    else -> YamlRawReader.parse(rawContent)
                }
                serializerRegistry.getOrDefault(format, clazz).deserialize(data, clazz)
            } catch (e: Exception) {
                return@synchronized ReloadResult.Failure(
                    path = path,
                    cause = e
                )
            }

            // 3. Validate
            val validationResult = validationPipeline.create(clazz).validate(instance)
            if (!validationResult.isValid) {
                val errors = (validationResult as cc.arccore.config.runtime.validation.ValidationResult.Invalid).errors
                return@synchronized ReloadResult.Failure(
                    path = path,
                    cause = cc.arccore.config.runtime.exception.ConfigValidationException(path, errors),
                    validationErrors = errors
                )
            }

            // 4. Increment generation and mark old handles stale
            val newGeneration = reloadGeneration.increment()
            for (handle in staleHandles) {
                handle.markStale()
            }

            val durationNanos = System.nanoTime() - startNanos
            ReloadResult.Success(path = path, newGeneration = newGeneration, durationNanos = durationNanos)
        }
    }

    private fun readContent(path: String): String? {
        return if (storagePort != null) {
            storagePort.readFile(moduleId, path)
        } else {
            null
        }
    }

    /**
     * Minimal JSON-to-Map parser for flat JSON objects.
     * This is not a general JSON parser; only handles `{ "key": value }` flat objects.
     */
    private fun parseJson(content: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val trimmed = content.trim().removePrefix("{").removeSuffix("}").trim()
        if (trimmed.isEmpty()) return result

        // Split by commas that are not inside strings — rudimentary but sufficient for flat configs
        var i = 0
        var inString = false
        val segments = mutableListOf<String>()
        val sb = StringBuilder()
        while (i < trimmed.length) {
            val ch = trimmed[i]
            when {
                ch == '"' -> {
                    inString = !inString
                    sb.append(ch)
                }
                ch == ',' && !inString -> {
                    segments.add(sb.toString().trim())
                    sb.clear()
                }
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
            result[key] = parseJsonValue(rawValue)
        }
        return result
    }

    private fun parseJsonValue(raw: String): Any? {
        return when {
            raw == "null" -> null
            raw == "true" -> true
            raw == "false" -> false
            raw.startsWith('"') && raw.endsWith('"') -> raw.substring(1, raw.length - 1)
            raw.toIntOrNull() != null -> raw.toInt()
            raw.toLongOrNull() != null -> raw.toLong()
            raw.toDoubleOrNull() != null -> raw.toDouble()
            else -> raw
        }
    }
}
