package cc.arccore.storage.runtime.ownership

import cc.arccore.storage.runtime.storage.AbstractStorageHandle

/**
 * Tracks all open [AbstractStorageHandle]s grouped by owning module.
 *
 * The registry is the single source of truth for which handles are alive,
 * enabling bulk-close operations during module unload or runtime shutdown.
 */
interface StorageOwnershipRegistry {

    /**
     * Registers [handle] under its [AbstractStorageHandle.moduleId].
     */
    fun register(handle: AbstractStorageHandle)

    /**
     * Closes and deregisters all handles owned by [moduleId].
     */
    fun closeAll(moduleId: String)

    /**
     * Closes and deregisters every handle in the registry.
     * Called during full runtime shutdown.
     */
    fun closeAllHandles()

    /**
     * Returns an immutable snapshot of all [StorageOwnership] records.
     */
    fun snapshot(): List<StorageOwnership>

    /**
     * Returns the number of handles currently tracked for [moduleId].
     */
    fun countFor(moduleId: String): Int
}
