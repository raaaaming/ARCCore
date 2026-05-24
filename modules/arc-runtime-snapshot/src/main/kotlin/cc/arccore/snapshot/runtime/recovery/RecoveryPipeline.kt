package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleEvent
import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleObserver
import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.model.RecoveryResult

internal class RecoveryPipeline(
    private val stages: List<RecoveryPipelineStage>,
    private val observers: List<SnapshotLifecycleObserver> = emptyList()
) {
    fun execute(context: RecoveryContext): RecoveryResult {
        notify(SnapshotLifecycleEvent.RecoveryStarted(context.targetRuntimeId, context.snapshot.id))

        for (stage in stages) {
            when (val result = executeStage(stage, context)) {
                is RecoveryStageResult.Success -> {
                    context.stageResults[stage.phase] = RecoveryContext.StageOutcome.Success
                }
                is RecoveryStageResult.Skipped -> {
                    context.stageResults[stage.phase] = RecoveryContext.StageOutcome.Skipped(result.reason)
                }
                is RecoveryStageResult.Failure -> {
                    if (!result.fatalToRecovery) {
                        context.stageResults[stage.phase] = RecoveryContext.StageOutcome.Failure(result.error)
                        continue
                    }
                    context.phase = RecoveryPhase.FAILED
                    notify(SnapshotLifecycleEvent.RecoveryFailed(
                        context.targetRuntimeId, context.snapshot.id, stage.phase, result.error
                    ))
                    return RecoveryResult.Failure(
                        runtimeId = context.targetRuntimeId,
                        snapshotId = context.snapshot.id,
                        phase = stage.phase,
                        error = result.error
                    )
                }
            }
        }

        context.phase = RecoveryPhase.COMPLETED
        notify(SnapshotLifecycleEvent.RecoveryCompleted(
            context.targetRuntimeId, context.snapshot.id, context.elapsedMs
        ))

        val ownershipRestored = context.stageResults[RecoveryPhase.RESTORE_OWNERSHIP] is RecoveryContext.StageOutcome.Success
        val stateEntries = context.snapshot.state.size

        return RecoveryResult.Success(
            runtimeId = context.targetRuntimeId,
            snapshotId = context.snapshot.id,
            totalDurationMs = context.elapsedMs,
            ownershipRestored = ownershipRestored,
            stateEntriesRestored = stateEntries
        )
    }

    private fun executeStage(stage: RecoveryPipelineStage, context: RecoveryContext): RecoveryStageResult =
        try { stage.execute(context) } catch (e: Exception) { RecoveryStageResult.Failure(e) }

    private fun notify(event: SnapshotLifecycleEvent) {
        observers.forEach { it.onSnapshotEvent(event) }
    }
}
