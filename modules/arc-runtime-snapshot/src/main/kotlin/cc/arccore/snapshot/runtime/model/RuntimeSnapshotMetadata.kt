package cc.arccore.snapshot.runtime.model

import java.time.Instant

data class RuntimeSnapshotMetadata(
    val snapshotId: SnapshotId,
    val runtimeId: String,
    val runtimeType: String,
    val capturedAt: Instant,
    val schemaVersion: Int = 1,
    val sizeBytes: Long = 0L,
    val ownershipIncluded: Boolean = false,
    val tags: Map<String, String> = emptyMap()
) {
    fun isCompatibleWith(schemaVersion: Int): Boolean = this.schemaVersion <= schemaVersion

    fun ageMs(): Long = Instant.now().toEpochMilli() - capturedAt.toEpochMilli()
}
