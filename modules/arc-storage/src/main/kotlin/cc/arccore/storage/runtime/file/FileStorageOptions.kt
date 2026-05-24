package cc.arccore.storage.runtime.file

/**
 * Options controlling how a [FileStorage] handle is initialised and accessed.
 *
 * @property createIfAbsent Create the file (and parent directories) if they do not exist.
 * @property readOnly Prevents write operations. Throws [cc.arccore.storage.runtime.exception.InvalidStorageAccessException] on any write attempt.
 */
data class FileStorageOptions(
    val createIfAbsent: Boolean = true,
    val readOnly: Boolean = false
) {
    companion object {
        /** Default options: create if absent, read-write. */
        val DEFAULT = FileStorageOptions()

        /** Read-only variant: file must already exist. */
        val READ_ONLY = FileStorageOptions(createIfAbsent = false, readOnly = true)
    }
}
