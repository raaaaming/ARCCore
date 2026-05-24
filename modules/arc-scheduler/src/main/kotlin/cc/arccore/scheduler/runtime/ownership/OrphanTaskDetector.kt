package cc.arccore.scheduler.runtime.ownership

import cc.arccore.scheduler.runtime.task.ScheduledTask
import cc.arccore.scheduler.runtime.task.TaskStatus

data class OrphanTaskReport(
    val moduleId: String,
    val orphanedTasks: List<ScheduledTask>,
    val severity: OrphanSeverity
)

enum class OrphanSeverity { NONE, WARNING, CRITICAL }

internal class OrphanTaskDetector(
    private val registry: TaskOwnershipRegistry,
    private val moduleGenerations: MutableMap<String, Int> = mutableMapOf()
) {
    fun recordGeneration(moduleId: String, generation: Int) {
        moduleGenerations[moduleId] = generation
    }

    fun detect(moduleId: String): OrphanTaskReport {
        val activeTasks = registry.getActiveTasks(moduleId)
        val orphans = activeTasks.filter { it.status == TaskStatus.RUNNING || it.status == TaskStatus.PENDING }

        val severity = when {
            orphans.isEmpty() -> OrphanSeverity.NONE
            orphans.any { it.type.isRepeating } -> OrphanSeverity.CRITICAL
            else -> OrphanSeverity.WARNING
        }

        return OrphanTaskReport(moduleId, orphans, severity)
    }

    fun detectAll(moduleIds: Set<String>): List<OrphanTaskReport> =
        moduleIds.map { detect(it) }.filter { it.severity != OrphanSeverity.NONE }
}
