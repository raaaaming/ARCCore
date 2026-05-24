package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.ownership.OwnershipTransferCoordinator

internal class OwnershipTransferStage(
    private val coordinator: OwnershipTransferCoordinator
) : PipelineStage {
    override val phase = ZeroDowntimePhase.OWNERSHIP_TRANSFER
    override val rollbackOnFailure = true

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.OWNERSHIP_TRANSFER

        return try {
            coordinator.transfer(context)
            StageResult.Success
        } catch (e: Exception) {
            StageResult.Failure(e)
        }
    }
}
