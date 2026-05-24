package cc.arccore.snapshot.runtime.model

sealed class RecoveryResult {
    data class Success(
        val runtimeId: String,
        val snapshotId: SnapshotId,
        val totalDurationMs: Long,
        val ownershipRestored: Boolean = false,
        val stateEntriesRestored: Int = 0
    ) : RecoveryResult()

    data class Failure(
        val runtimeId: String,
        val snapshotId: SnapshotId,
        val phase: RecoveryPhase,
        val error: Throwable,
        val rollbackSuccess: Boolean = false
    ) : RecoveryResult()

    data class Rejected(
        val runtimeId: String,
        val reason: String
    ) : RecoveryResult()
}
