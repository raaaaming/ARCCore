package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

class ClassLoaderCleanupStep : PrioritizedCleanupStep(
    priority = CleanupPriority.CLASSLOADER,
    body = { context ->
        val cl = context.classLoader
        if (cl == null) {
            CleanupStepResult.Skipped
        } else {
            try {
                cl.close()
                CleanupStepResult.Success
            } catch (e: Exception) {
                CleanupStepResult.Failure(e)
            }
        }
    }
)
