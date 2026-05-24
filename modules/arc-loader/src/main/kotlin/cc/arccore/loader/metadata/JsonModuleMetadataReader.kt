package cc.arccore.loader.metadata

import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.ParserInvoker
import cc.arccore.loader.metadata.exception.MetadataReadException
import cc.arccore.loader.metadata.validation.MetadataValidator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class JsonModuleMetadataReader(
    private val mapper: ObjectMapper = createDefaultMapper()
) : ModuleMetadataReader {

    companion object {
        private const val MANIFEST_PATH = "META-INF/arc-module.json"

        private val bridgeRegistered = AtomicBoolean(false)

        private val log: Logger = Logger.getLogger(JsonModuleMetadataReader::class.java.name)

        fun createDefaultMapper(): ObjectMapper {
            return JsonMapper.builder()
                .addModule(
                    KotlinModule.Builder()
                        .withReflectionCacheSize(512)
                        .configure(KotlinFeature.NullToEmptyCollection, true)
                        .configure(KotlinFeature.NullToEmptyMap, true)
                        .configure(KotlinFeature.NullIsSameAsDefault, true)
                        .build()
                )
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
        }

        fun registerJsonBridge(mapper: ObjectMapper = createDefaultMapper()) {
            if (!bridgeRegistered.compareAndSet(false, true)) return
            ParserInvoker.register(
                parseFn = { json -> JsonModuleMetadataReader(mapper).readFromString(json) },
                serializeFn = { desc -> mapper.writerWithDefaultPrettyPrinter().writeValueAsString(desc) }
            )
        }
    }

    init {
        registerJsonBridge(mapper)
    }

    override fun read(jarPath: Path): ModuleDescription {
        require(jarPath.toString().endsWith(".jar", ignoreCase = true)) {
            "Not a JAR file: $jarPath"
        }

        val manifest = try {
            readManifestFromJar(jarPath)
        } catch (e: Exception) {
            throw MetadataReadException(
                "Failed to read module manifest from JAR: $jarPath",
                e
            )
        }

        return MetadataValidator.validateToDescription(manifest)
    }

    override fun read(inputStream: InputStream): ModuleDescription {
        val manifest = try {
            inputStream.use { stream ->
                mapper.readValue<ArcModuleManifest>(stream)
            }
        } catch (e: Exception) {
            throw MetadataReadException(
                "Failed to read module manifest from input stream",
                e
            )
        }

        return MetadataValidator.validateToDescription(manifest)
    }

    internal fun readFromString(json: String): ModuleDescription {
        val manifest = try {
            mapper.readValue<ArcModuleManifest>(json)
        } catch (e: Exception) {
            throw MetadataReadException(
                "Failed to parse module manifest from JSON string",
                e
            )
        }
        return MetadataValidator.validateToDescription(manifest)
    }

    private fun readManifestFromJar(jarPath: Path): ArcModuleManifest {
        try {
            java.io.FileInputStream(jarPath.toFile()).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.replace('\\', '/') == MANIFEST_PATH) {
                            return mapper.readValue(zis)
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            log.fine("ZipInputStream fast path failed for $jarPath, falling back to FileSystem: ${e.message}")
        }

        return try {
            val uri = java.net.URI.create("jar:${jarPath.toUri()}")
            FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fs: FileSystem ->
                val manifestPath = fs.getPath(MANIFEST_PATH)
                if (!java.nio.file.Files.exists(manifestPath)) {
                    throw MetadataReadException(
                        "Manifest '$MANIFEST_PATH' not found in JAR: $jarPath"
                    )
                }
                java.nio.file.Files.newInputStream(manifestPath).use { stream ->
                    mapper.readValue<ArcModuleManifest>(stream)
                }
            }
        } catch (e: MetadataReadException) {
            throw e
        } catch (e: Exception) {
            throw MetadataReadException(
                "Failed to read '$MANIFEST_PATH' from JAR: $jarPath",
                e
            )
        }
    }
}
