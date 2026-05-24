package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase

internal class RestoreRuntimeStateStage(
    private val recoverableRuntimes: Map<String, RecoverableRuntime>
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.RESTORE_RUNTIME_STATE
    override val rollbackOnFailure = true

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.RESTORE_RUNTIME_STATE

        val runtime = recoverableRuntimes[context.targetRuntimeId]
            ?: return RecoveryStageResult.Skipped("No RecoverableRuntime registered for '${context.targetRuntimeId}'")

        if (!runtime.canRecoverFrom(context.snapshot)) {
            return RecoveryStageResult.Failure(
                IllegalStateException("Runtime '${context.targetRuntimeId}' cannot recover from snapshot '${context.snapshot.id}'")
            )
        }

        return when (val result = runtime.recover(context.snapshot)) {
            is RecoverableRuntime.RecoveryApplyResult.Success -> RecoveryStageResult.Success
            is RecoverableRuntime.RecoveryApplyResult.PartialSuccess -> RecoveryStageResult.Success
            is RecoverableRuntime.RecoveryApplyResult.Failure -> RecoveryStageResult.Failure(result.error)
        }
    }
}
