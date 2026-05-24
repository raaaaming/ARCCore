package cc.arccore.scheduler.runtime.diagnostics

import cc.arccore.scheduler.runtime.cancellation.CancellationRecord
import cc.arccore.scheduler.runtime.ownership.OrphanTaskDetector
import cc.arccore.scheduler.runtime.ownership.TaskOwnershipRegistry
import cc.arccore.scheduler.runtime.task.CoroutineTaskHandle
import cc.arccore.scheduler.runtime.task.RepeatingTaskHandle
import cc.arccore.scheduler.runtime.task.ScheduledTask
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class SchedulerDiagnosticsCollector(
    private val ownershipRegistry: TaskOwnershipRegistry,
    private val orphanDetector: OrphanTaskDetector? = null
) {
    private val totalScheduled = ConcurrentHashMap<String, AtomicLong>()
    private val totalCompleted = ConcurrentHashMap<String, AtomicLong>()
    private val totalCancelled = ConcurrentHashMap<String, AtomicLong>()
    private val totalFailed = ConcurrentHashMap<String, AtomicLong>()
    private val lastActivity = ConcurrentHashMap<String, Instant>()

    fun onTaskRegistered(task: ScheduledTask) {
        totalScheduled.computeIfAbsent(task.moduleId) { AtomicLong(0) }.incrementAndGet()
        lastActivity[task.moduleId] = Instant.now()
    }

    fun onTaskCompleted(task: ScheduledTask) {
        totalCompleted.computeIfAbsent(task.moduleId) { AtomicLong(0) }.incrementAndGet()
        lastActivity[task.moduleId] = Instant.now()
    }

    fun onTaskCancelled(task: ScheduledTask) {
        totalCancelled.computeIfAbsent(task.moduleId) { AtomicLong(0) }.incrementAndGet()
    }

    fun onAllCancelled(moduleId: String) {
        val active = ownershipRegistry.activeTaskCount(moduleId)
        totalCancelled.computeIfAbsent(moduleId) { AtomicLong(0) }.addAndGet(active.toLong())
    }

    fun getMetrics(moduleId: String): TaskMetrics {
        val activeTasks = ownershipRegistry.getActiveTasks(moduleId)
        return TaskMetrics(
            moduleId = moduleId,
            totalScheduled = totalScheduled[moduleId]?.get() ?: 0L,
            activeTasks = activeTasks.size,
            completedTasks = totalCompleted[moduleId]?.get() ?: 0L,
            cancelledTasks = totalCancelled[moduleId]?.get() ?: 0L,
            failedTasks = totalFailed[moduleId]?.get() ?: 0L,
            repeatingTaskCount = activeTasks.count { it is RepeatingTaskHandle },
            coroutineTaskCount = activeTasks.count { it is CoroutineTaskHandle },
            lastActivityAt = lastActivity[moduleId]
        )
    }

    fun generateReport(
        moduleId: String,
        cancellationHistory: List<CancellationRecord>
    ): SchedulerDiagnosticsReport {
        val metrics = getMetrics(moduleId)
        val activeTasks = ownershipRegistry.getActiveTasks(moduleId)
        val orphanReport = orphanDetector?.detect(moduleId)

        return SchedulerDiagnosticsReport(
            moduleId = moduleId,
            metrics = metrics,
            activeTasks = activeTasks,
            recentCancellations = cancellationHistory.takeLast(20),
            orphanReport = orphanReport
        )
    }
}
