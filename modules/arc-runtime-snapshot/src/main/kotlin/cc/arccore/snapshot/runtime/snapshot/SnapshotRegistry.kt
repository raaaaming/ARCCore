package cc.arccore.snapshot.runtime.snapshot

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SnapshotRegistry(private val maxSnapshotsPerRuntime: Int = 10) {
    private val snapshots = ConcurrentHashMap<String, CopyOnWriteArrayList<RuntimeSnapshot>>()

    fun store(snapshot: RuntimeSnapshot) {
        val list = snapshots.computeIfAbsent(snapshot.runtimeId) { CopyOnWriteArrayList() }
        list.add(snapshot)
        while (list.size > maxSnapshotsPerRuntime) list.removeAt(0)
    }

    fun get(snapshotId: SnapshotId): RuntimeSnapshot? =
        snapshots.values.flatten().find { it.id == snapshotId }

    fun getLatest(runtimeId: String): RuntimeSnapshot? =
        snapshots[runtimeId]?.lastOrNull()

    fun getAll(runtimeId: String): List<RuntimeSnapshot> =
        snapshots[runtimeId]?.toList() ?: emptyList()

    fun remove(snapshotId: SnapshotId): Boolean {
        snapshots.values.forEach { list ->
            val removed = list.removeIf { it.id == snapshotId }
            if (removed) return true
        }
        return false
    }

    fun removeAll(runtimeId: String) {
        snapshots.remove(runtimeId)
    }

    fun totalCount(): Int = snapshots.values.sumOf { it.size }

    fun runtimeIds(): Set<String> = snapshots.keys.toSet()
}
