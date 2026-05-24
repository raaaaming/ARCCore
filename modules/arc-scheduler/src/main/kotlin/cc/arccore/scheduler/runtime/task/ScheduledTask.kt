package cc.arccore.scheduler.runtime.task

import java.time.Instant

interface ScheduledTask {
    val taskId: String
    val moduleId: String
    val type: ScheduledTaskType
    val status: TaskStatus
    val scheduledAt: Instant

    fun cancel()
    fun isCancelled(): Boolean = status == TaskStatus.CANCELLED
    fun isActive(): Boolean = status.isActive()
}
