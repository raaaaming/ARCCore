package cc.arccore.snapshot.runtime.exception

open class RuntimeSnapshotException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class SnapshotSerializationException(
    val runtimeId: String,
    message: String,
    cause: Throwable? = null
) : RuntimeSnapshotException("Serialization failed for '$runtimeId': $message", cause)

class RuntimeRecoveryException(
    val snapshotId: String,
    val phase: String,
    message: String,
    cause: Throwable? = null
) : RuntimeSnapshotException("Recovery failed at phase '$phase' for snapshot '$snapshotId': $message", cause)

class OwnershipRecoveryException(
    val runtimeId: String,
    val ownershipType: String,
    message: String,
    cause: Throwable? = null
) : RuntimeSnapshotException("Ownership recovery failed for '$runtimeId' ($ownershipType): $message", cause)

class InvalidSnapshotException(
    val snapshotId: String,
    val reason: String
) : RuntimeSnapshotException("Invalid snapshot '$snapshotId': $reason")

class StaleSnapshotException(
    val snapshotId: String,
    val snapshotAge: Long,
    val maxAgeMs: Long
) : RuntimeSnapshotException("Snapshot '$snapshotId' is stale: age=${snapshotAge}ms, max=${maxAgeMs}ms")

class DuplicateRecoverySessionException(
    val runtimeId: String
) : RuntimeSnapshotException("Recovery session already active for '$runtimeId'")
