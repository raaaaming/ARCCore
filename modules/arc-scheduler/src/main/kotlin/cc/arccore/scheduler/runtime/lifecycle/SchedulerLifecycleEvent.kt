package cc.arccore.scheduler.runtime.lifecycle

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.task.ScheduledTask
import java.time.Instant

sealed class SchedulerLifecycleEvent {
    abstract val moduleId: String
    abstract val timestamp: Instant

    data class SchedulerStarted(
        override val moduleId: String,
        override val timestamp: Instant = Instant.now()
    ) : SchedulerLifecycleEvent()

    data class TaskRegistered(
        override val moduleId: String,
        val task: ScheduledTask,
        override val timestamp: Instant = Instant.now()
    ) : SchedulerLifecycleEvent()

    data class TaskCancelled(
        override val moduleId: String,
        val taskId: String,
        val reason: CancellationReason,
        override val timestamp: Instant = Instant.now()
    ) : SchedulerLifecycleEvent()

    data class SchedulerClosing(
        override val moduleId: String,
        val activeTaskCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : SchedulerLifecycleEvent()

    data class SchedulerClosed(
        override val moduleId: String,
        val cancelledTaskCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : SchedulerLifecycleEvent()
}
