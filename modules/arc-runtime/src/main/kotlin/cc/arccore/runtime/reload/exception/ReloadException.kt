package cc.arccore.runtime.reload.exception

open class ReloadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ReloadFailedException(val moduleId: String, val phase: String, cause: Throwable? = null)
    : ReloadException(
        "Module '$moduleId' reload failed at phase '$phase'" +
            if (cause != null) ": ${cause.message}" else "",
        cause
    )

class ReloadRollbackException(val moduleId: String, cause: Throwable? = null)
    : ReloadException("Rollback failed for '$moduleId'", cause)

class InvalidReloadStateException(val moduleId: String, val state: String)
    : ReloadException("Module '$moduleId' is in invalid state for reload: $state")

class DependencyReloadException(val moduleId: String, val dependentId: String, cause: Throwable? = null)
    : ReloadException("Failed to reload dependent '$dependentId' of '$moduleId'", cause)
