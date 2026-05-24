package cc.arccore.bootstrap.runtime.exception

class StartupOptimizationException(
    message: String,
    cause: Throwable? = null
) : BootstrapException(message, cause)
