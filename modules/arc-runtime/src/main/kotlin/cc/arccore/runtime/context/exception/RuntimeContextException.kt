package cc.arccore.runtime.context.exception

import cc.arccore.api.exception.ModuleException

open class RuntimeContextException(message: String, cause: Throwable? = null) : ModuleException(message, cause)

class InvalidModuleContextException(message: String, cause: Throwable? = null) : RuntimeContextException(message, cause)

class RuntimeAccessException(message: String, cause: Throwable? = null) : RuntimeContextException(message, cause)

class LifecycleViolationException(message: String, cause: Throwable? = null) : RuntimeContextException(message, cause)
