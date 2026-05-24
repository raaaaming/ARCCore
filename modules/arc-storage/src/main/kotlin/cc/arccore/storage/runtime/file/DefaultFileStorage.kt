package cc.arccore.storage.runtime.file

import cc.arccore.storage.runtime.exception.InvalidStorageAccessException
import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.storage.StorageType
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * NIO-based implementation of [FileStorage].
 *
 * Resolves [relativePath] against [basePath] and normalises the result
 * to prevent path-traversal. If [FileStorageOptions.createIfAbsent] is set
 * and the file does not exist, parent directories and the file are created
 * during construction.
 */
class DefaultFileStorage(
    override val moduleId: String,
    private val basePath: Path,
    private val relativePath: String,
    private val options: FileStorageOptions = FileStorageOptions.DEFAULT
) : AbstractStorageHandle(moduleId = moduleId, storageType = StorageType.FILE), FileStorage {

    override val resolvedPath: Path = run {
        val normalised = basePath.resolve(relativePath).normalize()
        if (!normalised.startsWith(basePath.normalize())) {
            throw InvalidStorageAccessException(
                "Storage path '$relativePath' escapes the module data directory."
            )
        }
        normalised
    }

    init {
        if (options.createIfAbsent && !Files.exists(resolvedPath)) {
            Files.createDirectories(resolvedPath.parent)
            Files.createFile(resolvedPath)
        }
    }

    override fun exists(): Boolean {
        checkOpen()
        return Files.exists(resolvedPath)
    }

    override fun readBytes(): ByteArray {
        checkOpen()
        return Files.readAllBytes(resolvedPath)
    }

    override fun writeBytes(data: ByteArray) {
        checkOpen()
        if (options.readOnly) {
            throw InvalidStorageAccessException("FileStorage '$relativePath' is read-only.")
        }
        Files.write(resolvedPath, data)
    }

    override fun openInputStream(): InputStream {
        checkOpen()
        return Files.newInputStream(resolvedPath)
    }

    override fun openOutputStream(append: Boolean): OutputStream {
        checkOpen()
        if (options.readOnly) {
            throw InvalidStorageAccessException("FileStorage '$relativePath' is read-only.")
        }
        return if (append) {
            Files.newOutputStream(resolvedPath, StandardOpenOption.APPEND)
        } else {
            Files.newOutputStream(resolvedPath)
        }
    }

    override fun delete(): Boolean {
        checkOpen()
        return Files.deleteIfExists(resolvedPath)
    }

    override fun size(): Long {
        checkOpen()
        return if (Files.exists(resolvedPath)) Files.size(resolvedPath) else 0L
    }
}
