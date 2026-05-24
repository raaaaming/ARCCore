package cc.arccore.storage.runtime.integration

import java.util.UUID

/**
 * Out-port for notifying the arc-diagnostics subsystem about storage handle events.
 *
 * Keeping this as a thin interface prevents arc-storage from taking a compile-time
 * dependency on arc-diagnostics. The bridge implementation lives in arc-core or a
 * dedicated integration module and is injected at runtime.
 */
interface DiagnosticsStorageBridgePort {

    /**
     * Called when a new storage handle is opened.
     *
     * @param moduleId    The owning module.
     * @param handleId    Unique identifier of the handle.
     * @param storageType String representation of the [cc.arccore.storage.runtime.storage.StorageType].
     */
    fun onHandleOpened(moduleId: String, handleId: UUID, storageType: String)

    /**
     * Called when a storage handle is closed or marked stale.
     *
     * @param moduleId The owning module.
     * @param handleId Unique identifier of the handle.
     */
    fun onHandleClosed(moduleId: String, handleId: UUID)
}
