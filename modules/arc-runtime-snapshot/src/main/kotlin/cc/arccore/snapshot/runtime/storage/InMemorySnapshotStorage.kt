package cc.arccore.snapshot.runtime.storage

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotId
import java.util.concurrent.ConcurrentHashMap

class InMemorySnapshotStorage : SnapshotStorageBackend {
    override val backendName = "in-memory"

    private val store = ConcurrentHashMap<String, RuntimeSnapshot>()

    override fun store(snapshot: RuntimeSnapshot) {
        store[snapshot.id.value] = snapshot
    }

    override fun load(snapshotId: SnapshotId): RuntimeSnapshot? = store[snapshotId.value]

    override fun delete(snapshotId: SnapshotId): Boolean = store.remove(snapshotId.value) != null

    override fun listByRuntime(runtimeId: String): List<SnapshotId> =
        store.values
            .filter { it.runtimeId == runtimeId }
            .sortedBy { it.capturedAt }
            .map { it.id }

    override fun exists(snapshotId: SnapshotId): Boolean = store.containsKey(snapshotId.value)

    fun totalCount(): Int = store.size

    fun totalSizeBytes(): Long = store.values.sumOf { it.metadata.sizeBytes }
}
