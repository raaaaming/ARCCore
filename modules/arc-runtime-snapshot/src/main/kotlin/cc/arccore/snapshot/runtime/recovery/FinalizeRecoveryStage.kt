package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.state.RecoverySessionRegistry

internal class FinalizeRecoveryStage(
    private val sessionRegistry: RecoverySessionRegistry
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.FINALIZE_RECOVERY
    override val rollbackOnFailure = false

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.FINALIZE_RECOVERY
        context.rollbackAvailable = false
        sessionRegistry.complete(context.targetRuntimeId)
        return RecoveryStageResult.Success
    }
}
