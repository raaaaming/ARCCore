package cc.arccore.zerodowntime.runtime.model

import java.time.Instant

data class ZeroDowntimeMetrics(
    val totalTransitions: Long,
    val successfulTransitions: Long,
    val failedTransitions: Long,
    val rolledBackTransitions: Long,
    val averageTransitionMs: Double,
    val averageDrainMs: Double,
    val totalOwnershipTransfers: Long,
    val lastTransitionAt: Instant?
) {
    companion object {
        val EMPTY = ZeroDowntimeMetrics(0, 0, 0, 0, 0.0, 0.0, 0, null)
    }
}
