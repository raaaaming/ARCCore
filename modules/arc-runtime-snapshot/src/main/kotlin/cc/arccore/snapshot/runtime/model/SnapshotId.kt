package cc.arccore.snapshot.runtime.model

import java.util.UUID

@JvmInline
value class SnapshotId(val value: String) {
    companion object {
        fun generate(): SnapshotId = SnapshotId(UUID.randomUUID().toString())
        fun of(runtimeId: String, timestamp: Long) = SnapshotId("$runtimeId-$timestamp")
    }
    override fun toString() = value
}
