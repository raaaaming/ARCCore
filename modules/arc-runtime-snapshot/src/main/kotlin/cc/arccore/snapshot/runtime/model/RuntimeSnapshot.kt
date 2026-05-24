package cc.arccore.snapshot.runtime.model

import java.time.Instant

data class RuntimeSnapshot(
    val id: SnapshotId,
    val runtimeId: String,
    val capturedAt: Instant,
    val state: Map<String, Any?>,
    val ownershipState: Map<String, Any?> = emptyMap(),
    val metadata: RuntimeSnapshotMetadata = RuntimeSnapshotMetadata(
        snapshotId = id,
        runtimeId = runtimeId,
        runtimeType = "generic",
        capturedAt = capturedAt,
        sizeBytes = 0L,
        ownershipIncluded = ownershipState.isNotEmpty()
    )
) {
    fun isEmpty(): Boolean = state.isEmpty() && ownershipState.isEmpty()

    fun hasOwnershipState(): Boolean = ownershipState.isNotEmpty()

    companion object {
        fun empty(runtimeId: String): RuntimeSnapshot {
            val now = Instant.now()
            val id = SnapshotId.of(runtimeId, now.toEpochMilli())
            return RuntimeSnapshot(
                id = id,
                runtimeId = runtimeId,
                capturedAt = now,
                state = emptyMap()
            )
        }
    }
}
