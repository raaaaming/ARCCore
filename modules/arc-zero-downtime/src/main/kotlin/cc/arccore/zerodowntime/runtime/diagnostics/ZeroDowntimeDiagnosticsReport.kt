package cc.arccore.zerodowntime.runtime.diagnostics

import cc.arccore.zerodowntime.runtime.model.OwnershipTransferStats
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import java.time.Instant

data class TransitionRecord(
    val generation: Int,
    val phase: ZeroDowntimePhase,
    val success: Boolean,
    val durationMs: Long,
    val drainDurationMs: Long,
    val rollbackOccurred: Boolean,
    val timestamp: Instant
)

data class ZeroDowntimeDiagnosticsReport(
    val moduleId: String,
    val transitionHistory: List<TransitionRecord>,
    val averageTransitionMs: Double,
    val averageDrainMs: Double,
    val successRate: Double,
    val lastOwnershipStats: OwnershipTransferStats?
)
