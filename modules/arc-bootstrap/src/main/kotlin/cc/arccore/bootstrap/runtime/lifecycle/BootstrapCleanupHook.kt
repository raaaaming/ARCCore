package cc.arccore.bootstrap.runtime.lifecycle

import cc.arccore.bootstrap.runtime.lazy.LazyBootstrapRegistry
import cc.arccore.bootstrap.runtime.state.BootstrapStateRegistry
import java.util.logging.Logger

/**
 * Cleanup hook that releases bootstrap resources when a module is unloaded
 * or when the server shuts down.
 *
 * Should be registered with arc-runtime's module cleanup system.
 */
class BootstrapCleanupHook(
    private val stateRegistry: BootstrapStateRegistry,
    private val lazyRegistry: LazyBootstrapRegistry,
    private val log: Logger = Logger.getLogger(BootstrapCleanupHook::class.java.name)
) {

    /**
     * Cleans up all bootstrap state for a single module.
     * Call this when a module is disabled/unloaded.
     */
    fun cleanupModule(moduleId: String) {
        log.fine("[ARCCore] Cleaning up bootstrap state for '$moduleId'")
        stateRegistry.clearModule(moduleId)
        lazyRegistry.remove(moduleId)
    }

    /**
     * Cleans up all bootstrap state for all modules.
     * Call this on server shutdown.
     */
    fun cleanupAll() {
        log.fine("[ARCCore] Cleaning up all bootstrap state")
        stateRegistry.clear()
        lazyRegistry.clear()
    }

    /**
     * Flushes all pending lazy wires before shutdown to surface any wiring errors early.
     */
    fun flushAllLazy(): Map<String, cc.arccore.bootstrap.runtime.lazy.ServiceWiringResult> {
        return lazyRegistry.flushAll()
    }
}
