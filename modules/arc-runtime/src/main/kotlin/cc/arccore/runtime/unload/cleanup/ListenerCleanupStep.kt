package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

class ListenerCleanupStep : PrioritizedCleanupStep(
    priority = CleanupPriority.LISTENER,
    body = { context ->
        var hasError: Throwable? = null
        val module = context.container.module
        if (module is CleanupAware) {
            try {
                module.onCleanupListener()
            } catch (e: Exception) {
                hasError = e
            }
        }
        if (hasError != null) CleanupStepResult.Failure(hasError)
        else CleanupStepResult.Success
    }
)
