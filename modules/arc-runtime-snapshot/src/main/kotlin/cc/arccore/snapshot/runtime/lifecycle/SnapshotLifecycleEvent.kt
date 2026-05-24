package cc.arccore.snapshot.runtime.lifecycle

import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.model.SnapshotId
import java.time.Instant

sealed class SnapshotLifecycleEvent {
    abstract val runtimeId: String
    abstract val timestamp: Instant

    data class SnapshotCaptured(
        override val runtimeId: String,
        val snapshotId: SnapshotId,
        val captureDurationMs: Long,
        val sizeBytes: Long,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()

    data class SnapshotCaptureFailed(
        override val runtimeId: String,
        val error: Throwable,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()

    data class RecoveryStarted(
        override val runtimeId: String,
        val snapshotId: SnapshotId,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()

    data class RecoveryCompleted(
        override val runtimeId: String,
        val snapshotId: SnapshotId,
        val totalDurationMs: Long,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()

    data class RecoveryFailed(
        override val runtimeId: String,
        val snapshotId: SnapshotId,
        val phase: RecoveryPhase,
        val error: Throwable,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()

    data class OwnershipRestored(
        override val runtimeId: String,
        val restoredCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : SnapshotLifecycleEvent()
}
