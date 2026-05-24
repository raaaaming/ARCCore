package cc.arccore.loader.loader

import cc.arccore.loader.loader.exception.ModuleDiscoveryException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path

class ModuleDiscovery {

    companion object {
        private val JAR_GLOB: String = "*.jar"
    }

    fun ensureModulesDirectory(directory: Path): Path {
        return try {
            Files.createDirectories(directory)
        } catch (e: Exception) {
            throw ModuleDiscoveryException(
                "Failed to create modules directory: $directory",
                e
            )
        }
    }

    fun discover(directory: Path): List<Path> {
        if (!Files.exists(directory)) {
            return emptyList()
        }
        if (!Files.isDirectory(directory)) {
            throw ModuleDiscoveryException(
                "Not a directory: $directory"
            )
        }

        val jars = mutableListOf<Path>()
        val ds: DirectoryStream<Path> = Files.newDirectoryStream(directory, JAR_GLOB)
        try {
            for (entry in ds) {
                if (Files.isRegularFile(entry)) {
                    jars.add(entry)
                }
            }
        } catch (e: Exception) {
            throw ModuleDiscoveryException(
                "Failed to scan modules directory: $directory",
                e
            )
        } finally {
            try {
                ds.close()
            } catch (_: Exception) {
            }
        }

        return jars.sortedBy { it.fileName.toString() }
    }
}
