package cc.arccore.storage.runtime.lifecycle

import cc.arccore.storage.runtime.ownership.StorageOwnershipRegistry

/**
 * Coordinates the ordered teardown of storage resources for a given module.
 *
 * The coordinator invokes registered [StorageLifecycleHook]s in registration
 * order before and after delegating the actual close operation to the
 * [StorageOwnershipRegistry].
 */
class StorageCleanupCoordinator(
    private val ownershipRegistry: StorageOwnershipRegistry
) {

    private val hooks = mutableListOf<StorageLifecycleHook>()

    /** Registers a lifecycle hook. Hooks are invoked in registration order. */
    fun addHook(hook: StorageLifecycleHook) {
        hooks.add(hook)
    }

    /**
     * Tears down all storage resources for [moduleId].
     *
     * 1. Fires [StorageLifecycleHook.beforeUnload] on each registered hook.
     * 2. Closes all handles via [StorageOwnershipRegistry.closeAll].
     * 3. Fires [StorageLifecycleHook.afterUnload] on each registered hook.
     *
     * Exceptions thrown by hooks or handle close are suppressed and do not
     * prevent subsequent steps from executing.
     */
    fun unloadModule(moduleId: String) {
        hooks.forEach { runCatching { it.beforeUnload(moduleId) } }
        ownershipRegistry.closeAll(moduleId)
        hooks.forEach { runCatching { it.afterUnload(moduleId) } }
    }
}
