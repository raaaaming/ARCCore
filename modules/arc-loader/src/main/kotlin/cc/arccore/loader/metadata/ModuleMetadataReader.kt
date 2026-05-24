package cc.arccore.loader.metadata

import cc.arccore.api.module.ModuleDescription
import java.io.InputStream
import java.nio.file.Path

/**
 * Reads module metadata from either a JAR file path or an [InputStream].
 *
 * Implementations are responsible for:
 * 1. Locating `META-INF/arc-module.json` inside the JAR (or reading a raw JSON stream).
 * 2. Deserializing the JSON into an [ArcModuleManifest].
 * 3. Validating the manifest via [MetadataValidator].
 * 4. Converting the validated manifest into a [ModuleDescription].
 */
interface ModuleMetadataReader {

    /**
     * Reads and validates the module descriptor from a JAR file on disk.
     *
     * @param jarPath Absolute or relative path to the module JAR file.
     * @return A validated [ModuleDescription].
     * @throws MetadataReadException if the JAR cannot be read or the JSON is malformed.
     * @throws MetadataValidationException if the manifest fails validation.
     */
    fun read(jarPath: Path): ModuleDescription

    /**
     * Reads and validates the module descriptor from an arbitrary JSON input stream.
     *
     * @param inputStream An open stream containing the JSON manifest.
     * @return A validated [ModuleDescription].
     * @throws MetadataReadException if the stream cannot be read or the JSON is malformed.
     * @throws MetadataValidationException if the manifest fails validation.
     */
    fun read(inputStream: InputStream): ModuleDescription
}
