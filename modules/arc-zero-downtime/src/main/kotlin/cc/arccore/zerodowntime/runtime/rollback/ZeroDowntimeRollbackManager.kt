package cc.arccore.zerodowntime.runtime.rollback

import cc.arccore.zerodowntime.runtime.coordination.RoutingCoordinator
import cc.arccore.zerodowntime.runtime.coordination.ShadowModuleRegistry
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.ownership.OwnershipTransferCoordinator

data class ZeroDowntimeRollbackResult(
    val success: Boolean,
    val restoredModules: List<String> = emptyList(),
    val failedRestores: List<Pair<String, Throwable>> = emptyList(),
    val newModuleCleanedUp: Boolean = false,
    val ownershipRestored: Boolean = false
)

internal class ZeroDowntimeRollbackManager(
    private val shadowRegistry: ShadowModuleRegistry,
    private val routingCoordinator: RoutingCoordinator,
    private val ownershipCoordinator: OwnershipTransferCoordinator
) {
    fun rollback(
        context: TransitionContext,
        failedPhase: ZeroDowntimePhase
    ): ZeroDowntimeRollbackResult {
        context.rollbackAvailable = false

        return when (failedPhase) {
            ZeroDowntimePhase.PREPARE -> rollbackFromPrepare(context)
            ZeroDowntimePhase.BOOTSTRAP_NEW -> rollbackFromBootstrapNew(context)
            ZeroDowntimePhase.VALIDATE -> rollbackFromBootstrapNew(context)
            ZeroDowntimePhase.OWNERSHIP_TRANSFER -> rollbackFromOwnershipTransfer(context)
            ZeroDowntimePhase.REQUEST_DRAIN -> rollbackFromRequestDrain(context)
            ZeroDowntimePhase.SWITCH_ROUTING -> rollbackFromSwitchRouting(context)
            ZeroDowntimePhase.CLEANUP_OLD -> ZeroDowntimeRollbackResult(
                success = false,
                failedRestores = listOf(Pair(context.targetModuleId, IllegalStateException("Cannot rollback after CLEANUP_OLD")))
            )
            ZeroDowntimePhase.COMPLETED,
            ZeroDowntimePhase.ROLLING_BACK,
            ZeroDowntimePhase.ABORTED,
            ZeroDowntimePhase.FAILED,
            ZeroDowntimePhase.IDLE -> ZeroDowntimeRollbackResult(
                success = false,
                failedRestores = listOf(Pair(context.targetModuleId,
                    IllegalStateException("Rollback called in invalid phase: $failedPhase")))
            )
        }
    }

    private fun rollbackFromPrepare(context: TransitionContext): ZeroDowntimeRollbackResult {
        return ZeroDowntimeRollbackResult(success = true)
    }

    private fun rollbackFromBootstrapNew(context: TransitionContext): ZeroDowntimeRollbackResult {
        val newModuleId = context.targetModuleId
        val cleaned = try {
            shadowRegistry.discard(newModuleId)
            true
        } catch (e: Exception) {
            false
        }
        return ZeroDowntimeRollbackResult(
            success = cleaned,
            newModuleCleanedUp = cleaned
        )
    }

    private fun rollbackFromOwnershipTransfer(context: TransitionContext): ZeroDowntimeRollbackResult {
        return try {
            ownershipCoordinator.rollback(context)
            rollbackFromBootstrapNew(context).copy(ownershipRestored = true)
        } catch (e: Exception) {
            ZeroDowntimeRollbackResult(
                success = false,
                failedRestores = listOf(Pair(context.targetModuleId, e))
            )
        }
    }

    private fun rollbackFromRequestDrain(context: TransitionContext): ZeroDowntimeRollbackResult {
        return rollbackFromOwnershipTransfer(context)
    }

    private fun rollbackFromSwitchRouting(context: TransitionContext): ZeroDowntimeRollbackResult {
        return try {
            routingCoordinator.rollback(context.targetModuleId, context)
            rollbackFromOwnershipTransfer(context)
        } catch (e: Exception) {
            ZeroDowntimeRollbackResult(
                success = false,
                failedRestores = listOf(Pair(context.targetModuleId, e))
            )
        }
    }
}
