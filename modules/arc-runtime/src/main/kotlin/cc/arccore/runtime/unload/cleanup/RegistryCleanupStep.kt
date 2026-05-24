package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

class RegistryCleanupStep(
    private val unregisterFromRegistry: (String) -> Unit
) : PrioritizedCleanupStep(
    priority = CleanupPriority.REGISTRY,
    body = { context ->
        try {
            unregisterFromRegistry(context.moduleId)
            CleanupStepResult.Success
        } catch (e: Exception) {
            CleanupStepResult.Failure(e)
        }
    }
)
