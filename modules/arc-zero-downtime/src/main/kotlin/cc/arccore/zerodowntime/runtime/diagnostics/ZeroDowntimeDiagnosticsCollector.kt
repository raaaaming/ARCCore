package cc.arccore.zerodowntime.runtime.diagnostics

import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleEvent
import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleObserver
import cc.arccore.zerodowntime.runtime.metrics.ZeroDowntimeMetricsAccumulator
import cc.arccore.zerodowntime.runtime.model.OwnershipTransferStats
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeMetrics
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class ZeroDowntimeDiagnosticsCollector(
    private val metricsAccumulator: ZeroDowntimeMetricsAccumulator,
    private val maxHistoryPerModule: Int = 50
) : ZeroDowntimeLifecycleObserver {

    private val transitionRecords = ConcurrentHashMap<String, CopyOnWriteArrayList<TransitionRecord>>()
    private val activeTransitionStarts = ConcurrentHashMap<String, Long>()
    private val lastOwnershipStats = ConcurrentHashMap<String, OwnershipTransferStats>()

    override fun onZeroDowntimeEvent(event: ZeroDowntimeLifecycleEvent) {
        when (event) {
            is ZeroDowntimeLifecycleEvent.TransitionStarted -> {
                activeTransitionStarts[event.moduleId] = event.timestamp.toEpochMilli()
            }
            is ZeroDowntimeLifecycleEvent.OwnershipTransferCompleted -> {
                lastOwnershipStats[event.moduleId] = event.stats
            }
            is ZeroDowntimeLifecycleEvent.TransitionCompleted -> {
                activeTransitionStarts.remove(event.moduleId)
                recordTransition(event.moduleId, ZeroDowntimePhase.COMPLETED, true, event.totalDurationMs, 0L, false)
                metricsAccumulator.recordSuccess(event.totalDurationMs, 0L)
            }
            is ZeroDowntimeLifecycleEvent.TransitionFailed -> {
                activeTransitionStarts.remove(event.moduleId)
                recordTransition(event.moduleId, event.phase, false, 0L, 0L, false)
                metricsAccumulator.recordFailure(false)
            }
            is ZeroDowntimeLifecycleEvent.TransitionRolledBack -> {
                recordTransition(event.moduleId, ZeroDowntimePhase.ROLLING_BACK, false, 0L, 0L, true)
                metricsAccumulator.recordFailure(true)
            }
            else -> {}
        }
    }

    private fun recordTransition(
        moduleId: String,
        phase: ZeroDowntimePhase,
        success: Boolean,
        durationMs: Long,
        drainDurationMs: Long,
        rollbackOccurred: Boolean
    ) {
        val records = transitionRecords.computeIfAbsent(moduleId) { CopyOnWriteArrayList() }
        records.add(TransitionRecord(
            generation = records.size,
            phase = phase,
            success = success,
            durationMs = durationMs,
            drainDurationMs = drainDurationMs,
            rollbackOccurred = rollbackOccurred,
            timestamp = java.time.Instant.now()
        ))
        while (records.size > maxHistoryPerModule) records.removeAt(0)
    }

    fun collectCurrentMetrics(): ZeroDowntimeMetrics = metricsAccumulator.snapshot()

    fun getDiagnosticsReport(moduleId: String): ZeroDowntimeDiagnosticsReport? {
        val records = transitionRecords[moduleId]?.toList() ?: return null
        if (records.isEmpty()) return null

        val successRecords = records.filter { it.success }
        val avgTransition = if (successRecords.isNotEmpty()) successRecords.map { it.durationMs }.average() else 0.0
        val avgDrain = if (successRecords.isNotEmpty()) successRecords.map { it.drainDurationMs }.average() else 0.0
        val successRate = if (records.isNotEmpty()) successRecords.size.toDouble() / records.size else 0.0

        return ZeroDowntimeDiagnosticsReport(
            moduleId = moduleId,
            transitionHistory = records,
            averageTransitionMs = avgTransition,
            averageDrainMs = avgDrain,
            successRate = successRate,
            lastOwnershipStats = lastOwnershipStats[moduleId]
        )
    }

    fun getAllReports(): Map<String, ZeroDowntimeDiagnosticsReport> {
        return transitionRecords.keys.mapNotNull { moduleId ->
            getDiagnosticsReport(moduleId)?.let { moduleId to it }
        }.toMap()
    }
}
