package cc.arccore.scheduler.runtime.task

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.scheduling.TickDuration
import kotlinx.coroutines.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class CoroutineTaskHandle(
    override val moduleId: String,
    override val type: ScheduledTaskType,
    val delay: TickDuration = TickDuration.ZERO,
    val period: TickDuration = TickDuration.ZERO,
    override val scheduledAt: Instant = Instant.now()
) : ScheduledTask {

    override val taskId: String = UUID.randomUUID().toString()

    private val _status = AtomicReference(TaskStatus.PENDING)
    override val status: TaskStatus get() = _status.get()

    private var _job: Job? = null

    fun attachJob(job: Job) {
        _job = job
        _status.compareAndSet(TaskStatus.PENDING, TaskStatus.RUNNING)
        job.invokeOnCompletion { cause ->
            if (cause == null) _status.compareAndSet(TaskStatus.RUNNING, TaskStatus.COMPLETED)
            else if (cause is CancellationException) {
                _status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLED)
            } else {
                _status.set(TaskStatus.FAILED)
            }
        }
    }

    override fun cancel() {
        cancelWithReason(CancellationReason.MANUAL)
    }

    internal fun cancelWithReason(reason: CancellationReason) {
        if (_status.get().isTerminal()) return
        if (_status.compareAndSet(TaskStatus.PENDING, TaskStatus.CANCELLED) ||
            _status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLED)) {
            _job?.cancel()
        }
    }
}
