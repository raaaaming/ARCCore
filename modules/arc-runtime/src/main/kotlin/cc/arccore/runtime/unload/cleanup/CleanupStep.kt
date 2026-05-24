package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

fun interface CleanupStep {

    fun cleanup(context: CleanupContext): CleanupStepResult
}

open class PrioritizedCleanupStep(
    val priority: CleanupPriority,
    private val body: (CleanupContext) -> CleanupStepResult
) : CleanupStep {
    override fun cleanup(context: CleanupContext): CleanupStepResult = body(context)

    companion object {
        fun of(priority: CleanupPriority, body: (CleanupContext) -> CleanupStepResult): PrioritizedCleanupStep {
            return PrioritizedCleanupStep(priority, body)
        }
    }
}

sealed class CleanupStepResult {
    data object Success : CleanupStepResult()
    data class Failure(val error: Throwable) : CleanupStepResult()
    data object Skipped : CleanupStepResult()
}

data class CleanupError(val key: String, val error: Throwable)
