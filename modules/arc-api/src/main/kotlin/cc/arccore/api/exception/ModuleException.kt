package cc.arccore.api.exception

/**
 * Base exception for all module-related errors in the ARCCore framework.
 */
open class ModuleException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when a module fails to load (e.g. missing dependencies, invalid descriptor).
 */
class ModuleLoadException(
    message: String,
    cause: Throwable? = null
) : ModuleException(message, cause)

/**
 * Thrown when a module fails to enable.
 */
class ModuleEnableException(
    message: String,
    cause: Throwable? = null
) : ModuleException(message, cause)

/**
 * Thrown when an illegal state transition is attempted on a module.
 */
class ModuleStateException(
    message: String,
    cause: Throwable? = null
) : ModuleException(message, cause)

/**
 * Thrown when a module dependency cannot be resolved.
 */
class ModuleDependencyException(
    message: String,
    cause: Throwable? = null
) : ModuleException(message, cause)
