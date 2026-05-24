package cc.arccore.storage.runtime.exception

/**
 * Thrown when an error occurs during storage cleanup or shutdown.
 */
class StorageCleanupException(
    message: String,
    cause: Throwable? = null
) : StorageRuntimeException(message, cause)
