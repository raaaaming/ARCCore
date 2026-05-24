package cc.arccore.snapshot.runtime.diagnostics

import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotMetrics

data class SnapshotDiagnosticsReport(
    val metrics: SnapshotMetrics,
    val activeRecoverySessions: Set<String>,
    val registeredSnapshotableRuntimes: Set<String>,
    val recentCaptureResults: List<SnapshotCaptureResult>,
    val recentRecoveryResults: List<RecoveryResult>
)
