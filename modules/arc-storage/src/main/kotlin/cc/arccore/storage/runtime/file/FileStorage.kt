package cc.arccore.storage.runtime.file

import cc.arccore.storage.runtime.storage.StorageHandle
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

/**
 * File-backed storage handle.
 *
 * Provides raw byte I/O and stream access for a single file path
 * scoped to a module's data directory.
 */
interface FileStorage : StorageHandle {

    /** Fully resolved absolute path of the managed file. */
    val resolvedPath: Path

    /** Returns `true` if the backing file exists on disk. */
    fun exists(): Boolean

    /** Reads and returns all bytes of the file. */
    fun readBytes(): ByteArray

    /**
     * Overwrites the file with [data].
     * @throws cc.arccore.storage.runtime.exception.InvalidStorageAccessException if read-only.
     */
    fun writeBytes(data: ByteArray)

    /** Opens a new [InputStream] for the file. Caller is responsible for closing it. */
    fun openInputStream(): InputStream

    /**
     * Opens a new [OutputStream] for the file.
     * @param append If `true`, data is appended; otherwise the file is truncated.
     * @throws cc.arccore.storage.runtime.exception.InvalidStorageAccessException if read-only.
     */
    fun openOutputStream(append: Boolean = false): OutputStream

    /** Deletes the file. Returns `true` if the file existed and was deleted. */
    fun delete(): Boolean

    /** Returns the file size in bytes, or 0 if the file does not exist. */
    fun size(): Long
}
