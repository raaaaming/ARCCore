package cc.arccore.storage.runtime.exception

/**
 * Base exception for all storage runtime errors.
 */
open class StorageRuntimeException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
