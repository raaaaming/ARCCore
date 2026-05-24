package cc.arccore.migration.runtime.diagnostics

import cc.arccore.migration.runtime.lifecycle.MigrationLifecycleEvent
import cc.arccore.migration.runtime.lifecycle.MigrationLifecycleObserver
import cc.arccore.migration.runtime.metrics.MigrationMetricsAccumulator
import cc.arccore.migration.runtime.model.MigrationMetrics
import cc.arccore.migration.runtime.model.MigrationPhase
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class MigrationDiagnosticsCollector(
    private val metricsAccumulator: MigrationMetricsAccumulator = MigrationMetricsAccumulator(),
    private val maxHistoryPerModule: Int = 50
) : MigrationLifecycleObserver {
    private val historyByModule = ConcurrentHashMap<String, CopyOnWriteArrayList<MigrationRecord>>()

    override fun onMigrationEvent(event: MigrationLifecycleEvent) {
        when (event) {
            is MigrationLifecycleEvent.MigrationCompleted -> {
                metricsAccumulator.recordSuccess(event.totalDurationMs, 0L, 0L)
                addRecord(
                    moduleId = event.moduleId,
                    migrationId = event.migrationId.value,
                    sourceNodeId = "",
                    targetNodeId = "",
                    phase = MigrationPhase.COMPLETED,
                    success = true,
                    durationMs = event.totalDurationMs,
                    rollbackOccurred = false,
                    timestamp = event.timestamp
                )
            }
            is MigrationLifecycleEvent.MigrationFailed -> {
                metricsAccumulator.recordFailure(false)
                addRecord(
                    moduleId = event.moduleId,
                    migrationId = event.migrationId.value,
                    sourceNodeId = "",
                    targetNodeId = "",
                    phase = event.phase,
                    success = false,
                    durationMs = 0L,
                    rollbackOccurred = false,
                    timestamp = event.timestamp
                )
            }
            is MigrationLifecycleEvent.MigrationAborted -> {
                metricsAccumulator.recordAborted()
                addRecord(
                    moduleId = event.moduleId,
                    migrationId = event.migrationId.value,
                    sourceNodeId = "",
                    targetNodeId = "",
                    phase = event.phase,
                    success = false,
                    durationMs = 0L,
                    rollbackOccurred = false,
                    timestamp = event.timestamp
                )
            }
            is MigrationLifecycleEvent.MigrationRolledBack -> {
                metricsAccumulator.recordFailure(true)
                addRecord(
                    moduleId = event.moduleId,
                    migrationId = event.migrationId.value,
                    sourceNodeId = "",
                    targetNodeId = "",
                    phase = event.phase,
                    success = false,
                    durationMs = 0L,
                    rollbackOccurred = true,
                    timestamp = event.timestamp
                )
            }
            else -> {}
        }
    }

    private fun addRecord(
        moduleId: String,
        migrationId: String,
        sourceNodeId: String,
        targetNodeId: String,
        phase: MigrationPhase,
        success: Boolean,
        durationMs: Long,
        rollbackOccurred: Boolean,
        timestamp: Instant
    ) {
        val list = historyByModule.computeIfAbsent(moduleId) { CopyOnWriteArrayList() }
        list.add(
            MigrationRecord(
                migrationId = migrationId,
                sourceNodeId = sourceNodeId,
                targetNodeId = targetNodeId,
                phase = phase,
                success = success,
                durationMs = durationMs,
                rollbackOccurred = rollbackOccurred,
                timestamp = timestamp
            )
        )
        if (list.size > maxHistoryPerModule) {
            list.removeAt(0)
        }
    }

    fun collectCurrentMetrics(): MigrationMetrics = metricsAccumulator.snapshot()

    fun getDiagnosticsReport(moduleId: String): MigrationDiagnosticsReport? {
        val history = historyByModule[moduleId] ?: return null
        val historyList = history.toList()
        if (historyList.isEmpty()) return null

        val successList = historyList.filter { it.success }
        val avgMigrationMs = if (successList.isNotEmpty()) successList.map { it.durationMs }.average() else 0.0
        val successRate = historyList.size.toDouble().let { total ->
            if (total > 0) successList.size.toDouble() / total else 0.0
        }

        return MigrationDiagnosticsReport(
            moduleId = moduleId,
            migrationHistory = historyList,
            averageMigrationMs = avgMigrationMs,
            averageDrainMs = 0.0,
            successRate = successRate,
            lastTransferStats = null
        )
    }

    fun getAllReports(): Map<String, MigrationDiagnosticsReport> {
        return historyByModule.keys.mapNotNull { moduleId ->
            getDiagnosticsReport(moduleId)?.let { moduleId to it }
        }.toMap()
    }
}
