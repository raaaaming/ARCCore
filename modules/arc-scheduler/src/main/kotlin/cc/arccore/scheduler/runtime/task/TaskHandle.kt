package cc.arccore.scheduler.runtime.task

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class TaskHandle(
    override val moduleId: String,
    override val type: ScheduledTaskType,
    private val platformCancelFn: () -> Unit,
    override val scheduledAt: Instant = Instant.now()
) : ScheduledTask {

    override val taskId: String = UUID.randomUUID().toString()

    private val _status = AtomicReference(TaskStatus.PENDING)
    override val status: TaskStatus get() = _status.get()

    var cancellationReason: CancellationReason? = null
        private set

    fun markRunning() {
        _status.compareAndSet(TaskStatus.PENDING, TaskStatus.RUNNING)
    }

    fun markCompleted() {
        _status.compareAndSet(TaskStatus.RUNNING, TaskStatus.COMPLETED)
    }

    fun markFailed() {
        _status.set(TaskStatus.FAILED)
    }

    override fun cancel() {
        cancelWithReason(CancellationReason.MANUAL)
    }

    internal fun cancelWithReason(reason: CancellationReason) {
        if (_status.get().isTerminal()) return
        cancellationReason = reason
        if (_status.compareAndSet(TaskStatus.PENDING, TaskStatus.CANCELLED) ||
            _status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLED)) {
            try { platformCancelFn() } catch (_: Exception) {}
        }
    }
}
