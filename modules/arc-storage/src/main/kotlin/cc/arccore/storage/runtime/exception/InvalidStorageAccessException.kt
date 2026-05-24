package cc.arccore.storage.runtime.exception

/**
 * Thrown when a storage handle is accessed after it has been closed or marked stale.
 */
class InvalidStorageAccessException(
    message: String,
    cause: Throwable? = null
) : StorageRuntimeException(message, cause)
