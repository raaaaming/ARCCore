package cc.arccore.zerodowntime.runtime.ownership

import cc.arccore.zerodowntime.runtime.model.TransitionContext

internal class ServiceOwnershipTransfer {
    fun transfer(
        oldModuleId: String,
        newModuleId: String,
        context: TransitionContext
    ): ServiceTransferResult {
        // Service 소유권 이전:
        // 1. OLD 서비스는 NEW 모듈 onEnable() 이후 자동으로 교체됨 (같은 타입이면)
        // 2. 실제 ServiceRegistry 접근은 통합 레이어(integration/)에서 수행
        // 현재: 전환 의도만 기록하고 통합 레이어에 위임
        context.ownershipTransferStats.serviceBindingsTransferred += 0
        return ServiceTransferResult.Skipped("Service transfer delegated to integration layer")
    }
}
