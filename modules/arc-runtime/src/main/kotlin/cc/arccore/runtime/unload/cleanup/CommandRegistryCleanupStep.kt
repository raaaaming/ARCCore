package cc.arccore.runtime.unload.cleanup

import cc.arccore.api.command.CommandRegistry
import cc.arccore.runtime.unload.CleanupContext
import java.util.logging.Logger

class CommandRegistryCleanupStep(
    private val commandRegistry: CommandRegistry
) : PrioritizedCleanupStep(
    priority = CleanupPriority.COMMAND,
    body = commandRegistryCleanupBody(commandRegistry)
) {
    companion object {
        private val log = Logger.getLogger(CommandRegistryCleanupStep::class.java.name)

        private fun commandRegistryCleanupBody(
            commandRegistry: CommandRegistry
        ): (CleanupContext) -> CleanupStepResult = { context ->
            try {
                val removed = commandRegistry.unregisterAllById(context.moduleId)
                if (removed > 0) {
                    log.fine("Unregistered $removed command(s) for module '${context.moduleId}'")
                }
                CleanupStepResult.Success
            } catch (e: Exception) {
                log.warning("Failed to unregister commands for '${context.moduleId}': ${e.message}")
                CleanupStepResult.Failure(e)
            }
        }
    }
}
