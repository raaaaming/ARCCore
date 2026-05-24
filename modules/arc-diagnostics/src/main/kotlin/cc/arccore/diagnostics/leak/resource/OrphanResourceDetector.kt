package cc.arccore.diagnostics.leak.resource

import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * Detects orphan resources (executors, schedulers) that survive module unload.
 *
 * Why orphan executors are dangerous:
 *   An ExecutorService holds its own thread pool. If a module creates one but never shuts it down
 *   on unload, those threads keep running, hold references to the old ClassLoader, and prevent GC.
 *   Worse: they may process tasks that reference stale module state, causing undefined behavior.
 *
 * Why orphan scheduler tasks are dangerous:
 *   Bukkit's scheduler runs tasks that may capture old listener/service instances in lambdas.
 *   Even after module unload, the captured closure holds a strong reference to the old classloader.
 *   This pins the entire module's class hierarchy in memory indefinitely.
 */
class OrphanResourceDetector {

    private val executorRegistry = ConcurrentHashMap<String, MutableSet<ExecutorService>>()
    private val schedulerTaskCounts = ConcurrentHashMap<String, Int>()

    fun registerExecutor(moduleId: String, executor: ExecutorService) {
        executorRegistry.getOrPut(moduleId) { ConcurrentHashMap.newKeySet() }.add(executor)
    }

    fun registerSchedulerTaskCount(moduleId: String, count: Int) {
        schedulerTaskCounts[moduleId] = count
    }

    fun detect(moduleId: String): List<LeakReport> {
        val reports = mutableListOf<LeakReport>()

        executorRegistry[moduleId]?.let { executors ->
            val activeCount = executors.count { !it.isShutdown && !it.isTerminated }
            if (activeCount > 0) {
                reports += LeakReport(
                    moduleId = moduleId,
                    type = LeakType.ORPHAN_EXECUTOR,
                    severity = LeakSeverity.HIGH,
                    description = "$activeCount executor(s) still active after module '$moduleId' unload — threads may pin old classloader",
                    details = mapOf(
                        "activeExecutors" to activeCount.toString(),
                        "totalRegistered" to executors.size.toString()
                    )
                )
            }
        }

        schedulerTaskCounts[moduleId]?.let { count ->
            if (count > 0) {
                reports += LeakReport(
                    moduleId = moduleId,
                    type = LeakType.ORPHAN_SCHEDULER_TASK,
                    severity = LeakSeverity.HIGH,
                    description = "$count Bukkit scheduler task(s) reported active after module '$moduleId' unload",
                    details = mapOf("taskCount" to count.toString())
                )
            }
        }

        return reports
    }

    fun cleanup(moduleId: String) {
        executorRegistry.remove(moduleId)
        schedulerTaskCounts.remove(moduleId)
    }

    fun cleanupAll() {
        executorRegistry.clear()
        schedulerTaskCounts.clear()
    }
}
