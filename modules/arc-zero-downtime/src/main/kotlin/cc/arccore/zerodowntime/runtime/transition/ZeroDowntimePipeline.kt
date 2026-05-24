package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleEvent
import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleObserver
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeReloadResult
import cc.arccore.zerodowntime.runtime.rollback.ZeroDowntimeRollbackManager

internal class ZeroDowntimePipeline(
    private val stages: List<PipelineStage>,
    private val rollbackManager: ZeroDowntimeRollbackManager,
    private val observers: List<ZeroDowntimeLifecycleObserver> = emptyList()
) {
    fun execute(context: TransitionContext): ZeroDowntimeReloadResult {
        notify(ZeroDowntimeLifecycleEvent.TransitionStarted(
            moduleId = context.targetModuleId,
            oldGeneration = context.oldHandle.generation
        ))

        for (stage in stages) {
            when (val result = executeStage(stage, context)) {
                is StageResult.Success -> continue
                is StageResult.Skipped -> continue
                is StageResult.Failure -> {
                    if (!result.fatalToTransition) continue
                    return handleFailure(context, stage.phase, result.error, stage.rollbackOnFailure)
                }
            }
        }

        context.phase = ZeroDowntimePhase.COMPLETED

        notify(ZeroDowntimeLifecycleEvent.TransitionCompleted(
            moduleId = context.targetModuleId,
            totalDurationMs = context.elapsedMs
        ))

        return ZeroDowntimeReloadResult.Success(
            moduleId = context.targetModuleId,
            transitionDurationMs = context.elapsedMs,
            drainDurationMs = context.drainRecord.drainDurationMs,
            ownershipTransferStats = context.ownershipTransferStats.toImmutable(),
            affectedModules = context.affectedModuleIds
        )
    }

    private fun executeStage(stage: PipelineStage, context: TransitionContext): StageResult {
        return try {
            stage.execute(context)
        } catch (e: Exception) {
            StageResult.Failure(e)
        }
    }

    private fun handleFailure(
        context: TransitionContext,
        phase: ZeroDowntimePhase,
        error: Throwable,
        shouldRollback: Boolean
    ): ZeroDowntimeReloadResult {
        context.phase = ZeroDowntimePhase.FAILED

        notify(ZeroDowntimeLifecycleEvent.TransitionFailed(
            moduleId = context.targetModuleId,
            phase = phase,
            error = error
        ))

        if (shouldRollback && context.rollbackAvailable) {
            context.phase = ZeroDowntimePhase.ROLLING_BACK
            val rollbackResult = rollbackManager.rollback(context, phase)

            notify(ZeroDowntimeLifecycleEvent.TransitionRolledBack(
                moduleId = context.targetModuleId,
                rollbackResult = rollbackResult
            ))

            return ZeroDowntimeReloadResult.Failure(
                moduleId = context.targetModuleId,
                phase = phase,
                error = error,
                rollbackSuccess = rollbackResult.success
            )
        }

        return ZeroDowntimeReloadResult.Failure(
            moduleId = context.targetModuleId,
            phase = phase,
            error = error,
            rollbackSuccess = false
        )
    }

    private fun notify(event: ZeroDowntimeLifecycleEvent) {
        observers.forEach { it.onZeroDowntimeEvent(event) }
    }
}
