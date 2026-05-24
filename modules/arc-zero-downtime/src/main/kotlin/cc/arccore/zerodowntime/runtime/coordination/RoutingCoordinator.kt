package cc.arccore.zerodowntime.runtime.coordination

import cc.arccore.zerodowntime.runtime.model.TransitionContext

data class RoutingFailure(val type: String, val reason: String)

data class RoutingResult(
    val servicesRouted: Int = 0,
    val commandsRouted: Int = 0,
    val subscriptionsSwitched: Int = 0,
    val failedRouting: List<RoutingFailure> = emptyList()
)

internal class RoutingCoordinator {
    fun switchToPrimary(
        oldModuleId: String,
        newModuleId: String,
        context: TransitionContext
    ): RoutingResult {
        // 실제 ServiceRegistry/CommandRegistry 접근은 integration 레이어에서 수행
        // 여기서는 전환 조율 로직만 처리
        return RoutingResult(
            servicesRouted = context.ownershipTransferStats.serviceBindingsTransferred,
            commandsRouted = context.ownershipTransferStats.commandsTransferred,
            subscriptionsSwitched = context.ownershipTransferStats.eventSubscriptionsTransferred
        )
    }

    fun rollback(
        oldModuleId: String,
        context: TransitionContext
    ) {
        // 롤백: routing을 OLD로 복원
        // 실제 구현은 integration 레이어에서 ServiceRegistry/CommandRegistry 역전환
    }
}
