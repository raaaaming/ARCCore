package cc.arccore.snapshot.runtime.storage

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotId

interface SnapshotStorageBackend {
    val backendName: String

    fun store(snapshot: RuntimeSnapshot)
    fun load(snapshotId: SnapshotId): RuntimeSnapshot?
    fun delete(snapshotId: SnapshotId): Boolean
    fun listByRuntime(runtimeId: String): List<SnapshotId>
    fun exists(snapshotId: SnapshotId): Boolean

    // 미래 확장 포인트: distributed/persistent storage
    fun supportsDistributed(): Boolean = false
    fun supportsPersistence(): Boolean = false
}
