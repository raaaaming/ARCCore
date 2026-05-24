package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.ownership.OwnershipRecoveryManager

internal class RestoreOwnershipStage(
    private val ownershipManager: OwnershipRecoveryManager
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.RESTORE_OWNERSHIP
    override val rollbackOnFailure = true

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.RESTORE_OWNERSHIP

        if (!context.snapshot.hasOwnershipState()) {
            return RecoveryStageResult.Skipped("No ownership state in snapshot")
        }

        return try {
            val result = ownershipManager.restoreOwnership(
                context.targetRuntimeId,
                context.snapshot.ownershipState
            )
            when (result) {
                is OwnershipRecoveryManager.OwnershipRestoreResult.Success ->
                    RecoveryStageResult.Success
                is OwnershipRecoveryManager.OwnershipRestoreResult.PartialSuccess ->
                    RecoveryStageResult.Success
                is OwnershipRecoveryManager.OwnershipRestoreResult.Failure ->
                    RecoveryStageResult.Failure(result.error)
            }
        } catch (e: Exception) {
            RecoveryStageResult.Failure(e)
        }
    }
}
