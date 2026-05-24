package cc.arccore.storage.runtime.lifecycle

/**
 * Callback interface for storage lifecycle events.
 *
 * Implementations are invoked by [StorageCleanupCoordinator] at well-defined
 * points during module shutdown.
 */
interface StorageLifecycleHook {

    /**
     * Called before storage handles for [moduleId] are closed.
     * Implementations may flush buffers or snapshot state here.
     */
    fun beforeUnload(moduleId: String)

    /**
     * Called after all storage handles for [moduleId] have been closed.
     * Implementations may emit metrics or audit log entries here.
     */
    fun afterUnload(moduleId: String)
}
