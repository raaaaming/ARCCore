package cc.arccore.storage.runtime.exception

/**
 * Thrown when configuration data fails validation.
 */
class ConfigValidationException(
    message: String,
    cause: Throwable? = null
) : StorageRuntimeException(message, cause)
