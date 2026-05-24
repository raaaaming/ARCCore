package cc.arccore.scheduler.runtime

import cc.arccore.scheduler.runtime.scheduling.TickDuration
import cc.arccore.scheduler.runtime.task.ScheduledTask

interface SchedulerRuntime {
    val moduleId: String

    // 단발 실행
    fun sync(task: () -> Unit): ScheduledTask
    fun async(task: suspend () -> Unit): ScheduledTask

    // 지연 실행
    fun delay(duration: TickDuration, task: () -> Unit): ScheduledTask
    fun delayAsync(duration: TickDuration, task: suspend () -> Unit): ScheduledTask

    // 반복 실행
    fun repeat(
        delay: TickDuration = TickDuration.ZERO,
        period: TickDuration,
        task: () -> Unit
    ): ScheduledTask

    fun repeatAsync(
        delay: TickDuration = TickDuration.ZERO,
        period: TickDuration,
        task: suspend () -> Unit
    ): ScheduledTask

    // 상태 조회
    fun activeTasks(): List<ScheduledTask>
    fun activeTaskCount(): Int

    // 전체 취소
    fun cancelAll()

    // 미래 확장 구조 (현재는 default no-op — 분산/영속 스케줄러 확장 포인트)
    fun supportsDistributed(): Boolean = false
    fun supportsPersistence(): Boolean = false
}
