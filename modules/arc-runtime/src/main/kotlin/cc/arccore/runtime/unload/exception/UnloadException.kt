package cc.arccore.runtime.unload.exception

open class UnloadException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class CleanupException(
    message: String,
    cause: Throwable? = null
) : UnloadException(message, cause)

class TeardownException(
    message: String,
    cause: Throwable? = null
) : UnloadException(message, cause)

class UnloadValidationException(
    message: String,
    cause: Throwable? = null
) : UnloadException(message, cause)
