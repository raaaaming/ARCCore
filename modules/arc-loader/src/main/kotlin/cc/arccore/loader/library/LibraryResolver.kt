package cc.arccore.loader.library

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

class LibraryResolver(private val cacheDir: Path) {

    private val log = Logger.getLogger(LibraryResolver::class.java.name)

    fun resolve(coordinates: List<String>): List<URL> {
        if (coordinates.isEmpty()) return emptyList()
        Files.createDirectories(cacheDir)
        return coordinates.mapNotNull { coord ->
            try {
                resolveOne(coord)
            } catch (e: Exception) {
                log.severe("Failed to resolve library '$coord': ${e.message}")
                null
            }
        }
    }

    private fun resolveOne(coordinate: String): URL {
        val parts = coordinate.split(":")
        require(parts.size == 3) {
            "Invalid Maven coordinate '$coordinate'. Expected 'groupId:artifactId:version'"
        }
        val (group, artifact, version) = parts
        val cacheFile = cacheDir.resolve("${group.replace('.', '-')}-$artifact-$version.jar")

        if (!Files.exists(cacheFile)) {
            log.info("Downloading library: $coordinate")
            download(group, artifact, version, cacheFile)
            log.info("Downloaded library: $coordinate")
        }

        return cacheFile.toUri().toURL()
    }

    private fun download(group: String, artifact: String, version: String, target: Path) {
        val groupPath = group.replace('.', '/')
        val url = "https://repo1.maven.org/maven2/$groupPath/$artifact/$version/$artifact-$version.jar"
        val tmpFile = target.resolveSibling("${target.fileName}.tmp")
        try {
            URL(url).openStream().use { input ->
                Files.copy(input, tmpFile, StandardCopyOption.REPLACE_EXISTING)
            }
            Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            runCatching { Files.deleteIfExists(tmpFile) }
            throw e
        }
    }
}
