package cc.arccore.snapshot.runtime.model

import java.time.Instant

data class SnapshotMetrics(
    val totalSnapshots: Long,
    val successfulSnapshots: Long,
    val failedSnapshots: Long,
    val totalRecoveries: Long,
    val successfulRecoveries: Long,
    val failedRecoveries: Long,
    val averageCaptureDurationMs: Double,
    val averageRecoveryDurationMs: Double,
    val totalStoredBytes: Long,
    val lastActivityAt: Instant?
) {
    companion object {
        val EMPTY = SnapshotMetrics(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0L, null)
    }
}
