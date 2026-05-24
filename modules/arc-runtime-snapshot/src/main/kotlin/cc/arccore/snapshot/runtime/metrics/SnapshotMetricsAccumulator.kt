package cc.arccore.snapshot.runtime.metrics

import cc.arccore.snapshot.runtime.model.SnapshotMetrics
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class SnapshotMetricsAccumulator {
    private val totalSnapshots = AtomicLong(0)
    private val successfulSnapshots = AtomicLong(0)
    private val failedSnapshots = AtomicLong(0)
    private val totalRecoveries = AtomicLong(0)
    private val successfulRecoveries = AtomicLong(0)
    private val failedRecoveries = AtomicLong(0)
    private val totalCaptureDurationMs = AtomicLong(0)
    private val totalRecoveryDurationMs = AtomicLong(0)
    private val totalStoredBytes = AtomicLong(0)
    private val lastActivityAt = AtomicReference<Instant?>(null)

    fun recordCaptureSuccess(durationMs: Long, sizeBytes: Long) {
        totalSnapshots.incrementAndGet()
        successfulSnapshots.incrementAndGet()
        totalCaptureDurationMs.addAndGet(durationMs)
        totalStoredBytes.addAndGet(sizeBytes)
        lastActivityAt.set(Instant.now())
    }

    fun recordCaptureFailure() {
        totalSnapshots.incrementAndGet()
        failedSnapshots.incrementAndGet()
        lastActivityAt.set(Instant.now())
    }

    fun recordRecoverySuccess(durationMs: Long) {
        totalRecoveries.incrementAndGet()
        successfulRecoveries.incrementAndGet()
        totalRecoveryDurationMs.addAndGet(durationMs)
        lastActivityAt.set(Instant.now())
    }

    fun recordRecoveryFailure() {
        totalRecoveries.incrementAndGet()
        failedRecoveries.incrementAndGet()
        lastActivityAt.set(Instant.now())
    }

    fun snapshot(): SnapshotMetrics {
        val successCaptures = successfulSnapshots.get()
        val successRecoveries = successfulRecoveries.get()
        return SnapshotMetrics(
            totalSnapshots = totalSnapshots.get(),
            successfulSnapshots = successCaptures,
            failedSnapshots = failedSnapshots.get(),
            totalRecoveries = totalRecoveries.get(),
            successfulRecoveries = successRecoveries,
            failedRecoveries = failedRecoveries.get(),
            averageCaptureDurationMs = if (successCaptures > 0) totalCaptureDurationMs.get().toDouble() / successCaptures else 0.0,
            averageRecoveryDurationMs = if (successRecoveries > 0) totalRecoveryDurationMs.get().toDouble() / successRecoveries else 0.0,
            totalStoredBytes = totalStoredBytes.get(),
            lastActivityAt = lastActivityAt.get()
        )
    }
}
