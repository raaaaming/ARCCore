package cc.arccore.zerodowntime.runtime.ownership

import cc.arccore.zerodowntime.runtime.model.TransitionContext

internal class SchedulerOwnershipTransfer {
    fun transfer(
        oldModuleId: String,
        newModuleId: String,
        context: TransitionContext
    ): SchedulerTransferResult {
        // Scheduler 소유권 이전은 복잡하다:
        // 1. OLD 모듈의 반복 task들은 NEW 모듈 onEnable() 이후 NEW에서 재등록됨
        // 2. ONE-SHOT task들은 draining 단계에서 완료 대기
        // 현재 구현: 카운트만 추적하고 실제 이전은 모듈 onEnable()에 위임
        // (이전 로직은 BukkitModuleScheduler API 접근이 필요하므로 통합 레이어에서 구현)
        return SchedulerTransferResult.Skipped("Scheduler tasks will be re-registered by new module onEnable()")
    }

    fun estimateInflightTasks(moduleId: String): Int = 0
}
