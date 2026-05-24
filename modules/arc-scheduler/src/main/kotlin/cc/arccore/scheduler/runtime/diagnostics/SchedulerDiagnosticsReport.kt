package cc.arccore.scheduler.runtime.diagnostics

import cc.arccore.scheduler.runtime.cancellation.CancellationRecord
import cc.arccore.scheduler.runtime.ownership.OrphanTaskReport
import cc.arccore.scheduler.runtime.task.ScheduledTask

data class SchedulerDiagnosticsReport(
    val moduleId: String,
    val metrics: TaskMetrics,
    val activeTasks: List<ScheduledTask>,
    val recentCancellations: List<CancellationRecord>,
    val orphanReport: OrphanTaskReport?
)
