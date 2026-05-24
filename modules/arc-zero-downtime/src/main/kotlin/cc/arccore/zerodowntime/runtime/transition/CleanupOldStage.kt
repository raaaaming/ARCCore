package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

internal class CleanupOldStage(
    private val oldRuntimeCleaner: OldRuntimeCleaner
) : PipelineStage {
    override val phase = ZeroDowntimePhase.CLEANUP_OLD
    override val rollbackOnFailure = false

    fun interface OldRuntimeCleaner {
        fun cleanup(moduleId: String, context: TransitionContext): CleanupResult
    }

    sealed class CleanupResult {
        data object Success : CleanupResult()
        data class PartialSuccess(val warnings: List<String>) : CleanupResult()
        data class Failure(val error: Throwable) : CleanupResult()
    }

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.CLEANUP_OLD
        context.rollbackAvailable = false

        return when (val result = oldRuntimeCleaner.cleanup(context.targetModuleId, context)) {
            is CleanupResult.Success -> StageResult.Success
            is CleanupResult.PartialSuccess -> StageResult.Success
            is CleanupResult.Failure -> StageResult.Failure(result.error, fatalToTransition = false)
        }
    }
}
