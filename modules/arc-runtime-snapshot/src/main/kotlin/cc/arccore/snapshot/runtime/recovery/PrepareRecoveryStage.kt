package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.state.RecoverySessionRegistry
import cc.arccore.snapshot.runtime.validation.SnapshotValidator

internal class PrepareRecoveryStage(
    private val sessionRegistry: RecoverySessionRegistry,
    private val validator: SnapshotValidator
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.PREPARE_RECOVERY
    override val rollbackOnFailure = true

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.PREPARE_RECOVERY

        if (sessionRegistry.isRecovering(context.targetRuntimeId)) {
            return RecoveryStageResult.Failure(
                cc.arccore.snapshot.runtime.exception.DuplicateRecoverySessionException(context.targetRuntimeId)
            )
        }

        val validationError = validator.validate(context.snapshot)
        if (validationError != null) {
            return RecoveryStageResult.Failure(validationError)
        }

        sessionRegistry.begin(context.targetRuntimeId)
        return RecoveryStageResult.Success
    }
}
