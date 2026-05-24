package cc.arccore.zerodowntime.runtime.ownership

import cc.arccore.zerodowntime.runtime.model.OwnershipTransferStats
import cc.arccore.zerodowntime.runtime.model.TransitionContext

data class OwnershipTransferResult(
    val schedulerResult: SchedulerTransferResult,
    val serviceResult: ServiceTransferResult,
    val subscriptionResult: SubscriptionTransferResult,
    val commandResult: CommandTransferResult,
    val stats: OwnershipTransferStats = OwnershipTransferStats.EMPTY
)

internal class OwnershipTransferCoordinator(
    private val schedulerTransfer: SchedulerOwnershipTransfer = SchedulerOwnershipTransfer(),
    private val serviceTransfer: ServiceOwnershipTransfer = ServiceOwnershipTransfer(),
    private val subscriptionTransfer: SubscriptionOwnershipTransfer = SubscriptionOwnershipTransfer(),
    private val commandTransfer: CommandOwnershipTransfer = CommandOwnershipTransfer()
) {
    fun transfer(context: TransitionContext): OwnershipTransferResult {
        val startTime = System.currentTimeMillis()

        val schedulerResult = schedulerTransfer.transfer(
            context.targetModuleId,
            "${context.targetModuleId}-new",
            context
        )

        val serviceResult = serviceTransfer.transfer(
            context.targetModuleId,
            "${context.targetModuleId}-new",
            context
        )

        val subscriptionResult = subscriptionTransfer.transfer(
            context.targetModuleId,
            "${context.targetModuleId}-new",
            context
        )

        val commandResult = commandTransfer.transfer(
            context.targetModuleId,
            "${context.targetModuleId}-new",
            context
        )

        val transferDuration = System.currentTimeMillis() - startTime
        context.ownershipTransferStats.transferDurationMs = transferDuration

        return OwnershipTransferResult(
            schedulerResult = schedulerResult,
            serviceResult = serviceResult,
            subscriptionResult = subscriptionResult,
            commandResult = commandResult,
            stats = context.ownershipTransferStats.toImmutable()
        )
    }

    fun rollback(context: TransitionContext) {
        subscriptionTransfer.finalizeCancellation(context.targetModuleId)
    }
}
