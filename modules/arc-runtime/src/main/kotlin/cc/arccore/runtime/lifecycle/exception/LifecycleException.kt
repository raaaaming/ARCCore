package cc.arccore.runtime.lifecycle.exception

open class LifecycleException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ModuleEnableException(
    message: String,
    cause: Throwable? = null
) : LifecycleException(message, cause)

class ModuleDisableException(
    message: String,
    cause: Throwable? = null
) : LifecycleException(message, cause)

class ModuleUnloadException(
    message: String,
    cause: Throwable? = null
) : LifecycleException(message, cause)

class LifecycleRollbackException(
    message: String,
    cause: Throwable? = null
) : LifecycleException(message, cause)

class LifecycleValidationException(
    message: String,
    cause: Throwable? = null
) : LifecycleException(message, cause)
