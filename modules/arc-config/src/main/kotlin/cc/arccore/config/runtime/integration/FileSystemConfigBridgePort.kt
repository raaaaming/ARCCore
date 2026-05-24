package cc.arccore.config.runtime.integration

import cc.arccore.config.runtime.exception.ConfigRuntimeException
import java.nio.file.Files
import java.nio.file.Path

/**
 * [StorageConfigBridgePort] implemented with [java.nio.file.Files].
 *
 * No Bukkit/Paper dependency. Relative paths are resolved against [basePath].
 * A path traversal guard ensures resolved paths stay inside [basePath].
 *
 * @param basePath Root data directory for all config I/O.
 */
class FileSystemConfigBridgePort(private val basePath: Path) : StorageConfigBridgePort {

    private val normalizedBase: Path = basePath.toAbsolutePath().normalize()

    override fun readFile(moduleId: String, relativePath: String): String? {
        val resolved = resolve(moduleId, relativePath)
        if (!Files.exists(resolved)) return null
        return Files.readString(resolved, Charsets.UTF_8)
    }

    override fun writeFile(moduleId: String, relativePath: String, content: String) {
        val resolved = resolve(moduleId, relativePath)
        Files.createDirectories(resolved.parent)
        Files.writeString(resolved, content, Charsets.UTF_8)
    }

    override fun fileExists(moduleId: String, relativePath: String): Boolean {
        val resolved = resolve(moduleId, relativePath)
        return Files.exists(resolved)
    }

    private fun resolve(moduleId: String, relativePath: String): Path {
        val candidate = normalizedBase
            .resolve(moduleId)
            .resolve(relativePath)
            .toAbsolutePath()
            .normalize()

        if (!candidate.startsWith(normalizedBase)) {
            throw ConfigRuntimeException(
                "Path traversal detected: '$relativePath' for module '$moduleId' resolves outside base directory"
            )
        }
        return candidate
    }
}
