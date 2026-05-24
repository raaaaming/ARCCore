package cc.arccore.scheduler.runtime.diagnostics

import java.time.Instant

data class TaskMetrics(
    val moduleId: String,
    val totalScheduled: Long,
    val activeTasks: Int,
    val completedTasks: Long,
    val cancelledTasks: Long,
    val failedTasks: Long,
    val repeatingTaskCount: Int,
    val coroutineTaskCount: Int,
    val lastActivityAt: Instant?
)
