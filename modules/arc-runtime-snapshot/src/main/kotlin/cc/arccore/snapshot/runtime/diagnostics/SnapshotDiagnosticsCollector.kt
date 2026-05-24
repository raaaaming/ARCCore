package cc.arccore.snapshot.runtime.diagnostics

import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleEvent
import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleObserver
import cc.arccore.snapshot.runtime.metrics.SnapshotMetricsAccumulator
import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotMetrics
import java.util.concurrent.CopyOnWriteArrayList

class SnapshotDiagnosticsCollector(
    private val metricsAccumulator: SnapshotMetricsAccumulator = SnapshotMetricsAccumulator()
) : SnapshotLifecycleObserver {

    private val recentCaptures = CopyOnWriteArrayList<SnapshotCaptureResult>()
    private val recentRecoveries = CopyOnWriteArrayList<RecoveryResult>()
    private val maxHistory = 50

    override fun onSnapshotEvent(event: SnapshotLifecycleEvent) {
        when (event) {
            is SnapshotLifecycleEvent.SnapshotCaptured -> {
                metricsAccumulator.recordCaptureSuccess(event.captureDurationMs, event.sizeBytes)
            }
            is SnapshotLifecycleEvent.SnapshotCaptureFailed -> {
                metricsAccumulator.recordCaptureFailure()
            }
            is SnapshotLifecycleEvent.RecoveryCompleted -> {
                metricsAccumulator.recordRecoverySuccess(event.totalDurationMs)
            }
            is SnapshotLifecycleEvent.RecoveryFailed -> {
                metricsAccumulator.recordRecoveryFailure()
            }
            else -> {}
        }
    }

    fun recordCapture(result: SnapshotCaptureResult) {
        recentCaptures.add(result)
        while (recentCaptures.size > maxHistory) recentCaptures.removeAt(0)
    }

    fun recordRecovery(result: RecoveryResult) {
        recentRecoveries.add(result)
        while (recentRecoveries.size > maxHistory) recentRecoveries.removeAt(0)
    }

    fun getMetrics(): SnapshotMetrics = metricsAccumulator.snapshot()

    fun generateReport(
        activeRecoverySessions: Set<String>,
        registeredRuntimes: Set<String>
    ): SnapshotDiagnosticsReport = SnapshotDiagnosticsReport(
        metrics = getMetrics(),
        activeRecoverySessions = activeRecoverySessions,
        registeredSnapshotableRuntimes = registeredRuntimes,
        recentCaptureResults = recentCaptures.takeLast(20),
        recentRecoveryResults = recentRecoveries.takeLast(20)
    )
}
