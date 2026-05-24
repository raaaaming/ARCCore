package cc.arccore.runtime.unload

import cc.arccore.runtime.unload.cleanup.CleanupPriority
import cc.arccore.runtime.unload.cleanup.CleanupStep
import cc.arccore.runtime.unload.cleanup.CleanupStepResult
import cc.arccore.runtime.unload.cleanup.PrioritizedCleanupStep
import java.util.logging.Logger

class ModuleCleanupPipeline {

    private val log = Logger.getLogger(ModuleCleanupPipeline::class.java.name)
    private val steps = mutableMapOf<CleanupPriority, PrioritizedCleanupStep>()

    fun install(step: PrioritizedCleanupStep) {
        steps[step.priority] = step
    }

    fun installAll(steps: List<PrioritizedCleanupStep>) {
        for (step in steps) {
            this.steps[step.priority] = step
        }
    }

    fun replace(step: PrioritizedCleanupStep) {
        steps[step.priority] = step
    }

    fun remove(priority: CleanupPriority) {
        steps.remove(priority)
    }

    fun execute(context: CleanupContext): CleanupReport {
        val results = mutableListOf<CleanupStepReport>()
        val ordered = CleanupPriority.sortedAscending()

        for (priority in ordered) {
            val step = steps[priority] ?: continue
            log.fine("Running cleanup step ${priority.name} for module '${context.moduleId}'")

            val result = try {
                step.cleanup(context)
            } catch (e: Exception) {
                CleanupStepResult.Failure(e)
            }

            when (result) {
                is CleanupStepResult.Success -> {
                    log.fine("Cleanup step ${priority.name} completed for '${context.moduleId}'")
                    results.add(CleanupStepReport(priority.name, true, null))
                }
                is CleanupStepResult.Failure -> {
                    log.warning("Cleanup step ${priority.name} failed for '${context.moduleId}': ${result.error.message}")
                    results.add(CleanupStepReport(priority.name, false, result.error))
                }
                is CleanupStepResult.Skipped -> {
                    results.add(CleanupStepReport(priority.name, true, null, skipped = true))
                }
            }
        }

        val allSuccessful = results.all { it.success }
        return CleanupReport(context.moduleId, results, allSuccessful)
    }

    fun clear() {
        steps.clear()
    }

    data class CleanupStepReport(
        val stepName: String,
        val success: Boolean,
        val error: Throwable?,
        val skipped: Boolean = false
    )

    data class CleanupReport(
        val moduleId: String,
        val stepReports: List<CleanupStepReport>,
        val allSuccessful: Boolean
    ) {
        val failedSteps: List<CleanupStepReport> get() = stepReports.filter { !it.success }
        val skippedSteps: List<CleanupStepReport> get() = stepReports.filter { it.skipped }
    }
}
