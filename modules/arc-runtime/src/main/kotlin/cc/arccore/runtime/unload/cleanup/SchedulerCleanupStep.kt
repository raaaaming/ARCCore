package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

class SchedulerCleanupStep : PrioritizedCleanupStep(
    priority = CleanupPriority.SCHEDULER,
    body = { context ->
        var hasError: Throwable? = null
        val module = context.container.module
        if (module is CleanupAware) {
            try {
                module.onCleanupScheduler()
            } catch (e: Exception) {
                hasError = e
            }
        }
        if (hasError != null) CleanupStepResult.Failure(hasError)
        else CleanupStepResult.Success
    }
)
