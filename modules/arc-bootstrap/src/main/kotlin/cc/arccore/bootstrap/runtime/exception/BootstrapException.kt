package cc.arccore.bootstrap.runtime.exception

open class BootstrapException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
