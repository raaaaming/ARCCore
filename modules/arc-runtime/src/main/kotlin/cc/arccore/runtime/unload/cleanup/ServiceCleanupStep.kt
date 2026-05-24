package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

class ServiceCleanupStep : PrioritizedCleanupStep(
    priority = CleanupPriority.SERVICE,
    body = { context ->
        var hasError: Throwable? = null
        val module = context.container.module
        if (module is CleanupAware) {
            try {
                module.onCleanupService()
            } catch (e: Exception) {
                hasError = e
            }
        }
        if (hasError != null) CleanupStepResult.Failure(hasError)
        else CleanupStepResult.Success
    }
)
