package cc.arccore.scheduler.runtime.task

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.scheduling.TickDuration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

internal class RepeatingTaskHandle(
    override val moduleId: String,
    override val type: ScheduledTaskType,
    val delay: TickDuration,
    val period: TickDuration,
    platformCancelFn: () -> Unit,
    override val scheduledAt: Instant = Instant.now()
) : ScheduledTask {

    private val delegate = TaskHandle(moduleId, type, platformCancelFn, scheduledAt)

    override val taskId: String get() = delegate.taskId
    override val status: TaskStatus get() = delegate.status

    private val _executionCount = AtomicLong(0)
    val executionCount: Long get() = _executionCount.get()

    fun onExecute() {
        delegate.markRunning()
        _executionCount.incrementAndGet()
    }

    override fun cancel() = delegate.cancel()

    internal fun cancelWithReason(reason: CancellationReason) =
        delegate.cancelWithReason(reason)
}
