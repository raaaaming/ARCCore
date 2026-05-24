package cc.arccore.snapshot.runtime.cleanup

import cc.arccore.snapshot.runtime.coordination.SnapshotCoordinator
import cc.arccore.snapshot.runtime.snapshot.SnapshotRegistry
import cc.arccore.snapshot.runtime.state.RecoverySessionRegistry
import cc.arccore.snapshot.runtime.storage.SnapshotStorageBackend

data class SnapshotCleanupResult(
    val runtimeId: String,
    val snapshotsRemoved: Int,
    val recoverySessionCleaned: Boolean
)

class SnapshotCleanupManager internal constructor(
    private val registry: SnapshotRegistry,
    private val storage: SnapshotStorageBackend,
    private val sessionRegistry: RecoverySessionRegistry,
    private val coordinator: SnapshotCoordinator
) {
    fun cleanupRuntime(runtimeId: String): SnapshotCleanupResult {
        val snapshots = registry.getAll(runtimeId)
        snapshots.forEach { storage.delete(it.id) }
        registry.removeAll(runtimeId)
        coordinator.unregister(runtimeId)

        val sessionCleaned = sessionRegistry.isRecovering(runtimeId)
        if (sessionCleaned) sessionRegistry.complete(runtimeId)

        return SnapshotCleanupResult(
            runtimeId = runtimeId,
            snapshotsRemoved = snapshots.size,
            recoverySessionCleaned = sessionCleaned
        )
    }

    fun cleanupExpiredSnapshots(maxAgeMs: Long): Int {
        var removed = 0
        registry.runtimeIds().forEach { runtimeId ->
            registry.getAll(runtimeId)
                .filter { it.metadata.ageMs() > maxAgeMs }
                .forEach { snapshot ->
                    storage.delete(snapshot.id)
                    registry.remove(snapshot.id)
                    removed++
                }
        }
        return removed
    }
}
