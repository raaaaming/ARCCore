package cc.arccore.storage.runtime.diagnostics

import cc.arccore.storage.runtime.ownership.StorageOwnershipRegistry

/**
 * Default [StorageDiagnostics] implementation backed by a [StorageOwnershipRegistry].
 */
class DefaultStorageDiagnostics(
    private val ownershipRegistry: StorageOwnershipRegistry
) : StorageDiagnostics {

    override fun snapshot(): StorageSnapshot {
        val ownerships = ownershipRegistry.snapshot()
        return StorageSnapshot(ownerships = ownerships)
    }

    override fun snapshotForModule(moduleId: String): StorageSnapshot {
        val ownerships = ownershipRegistry.snapshot().filter { it.moduleId == moduleId }
        return StorageSnapshot(ownerships = ownerships)
    }

    override fun totalOpenHandles(): Int =
        ownershipRegistry.snapshot().size

    override fun openHandlesFor(moduleId: String): Int =
        ownershipRegistry.countFor(moduleId)
}
