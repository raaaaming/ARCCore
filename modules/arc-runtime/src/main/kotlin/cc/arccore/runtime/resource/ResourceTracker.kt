package cc.arccore.runtime.resource

import cc.arccore.runtime.resource.cleanup.ResourceCleanupResult
import cc.arccore.runtime.resource.snapshot.ModuleResourceSnapshot
import cc.arccore.runtime.resource.snapshot.ResourceSnapshot

/**
 * Global runtime resource ownership tracker.
 *
 * Why ownership-based tracking:
 *   In a hot-reload environment, modules come and go at runtime. Without an ownership graph,
 *   it is impossible to know WHICH resources belong to a given module and therefore WHICH
 *   resources need cleanup when that module unloads. Traditional try/finally patterns rely on
 *   the module developer to remember every resource — a single forgotten executor leaves its
 *   threads running and its ClassLoader unreachable by GC indefinitely.
 *
 * Why a global singleton (not per-module):
 *   A global tracker enables cross-module visibility. You can answer "which modules have
 *   active executors?" or "how many database connections are owned by arc-economy?" from a
 *   single query. Per-module trackers cannot answer these questions without coordination.
 *
 * Executor / ClassLoader leak chain:
 *   executor.submit { someModuleServiceCall() }  ← lambda captures old service instance
 *   old service instance.getClass().getClassLoader()  ← pinned ModuleClassLoader
 *   GC cannot collect ClassLoader  ← all module classes remain in metaspace forever
 *   Over N reloads: N * moduleSize metaspace permanently consumed → OutOfMemoryError
 */
interface ResourceTracker {

    /**
     * Track an AutoCloseable resource owned by [moduleId].
     * Returns a [ResourceHandle]; releasing the handle invokes [resource].close().
     */
    fun track(
        moduleId: String,
        name: String,
        type: ResourceType,
        resource: AutoCloseable
    ): ResourceHandle

    /**
     * Track a resource with a custom cleanup function.
     */
    fun track(
        moduleId: String,
        name: String,
        type: ResourceType,
        cleanup: () -> Unit
    ): ResourceHandle

    /**
     * Release all resources owned by [moduleId]. Called automatically on module unload.
     */
    fun releaseModule(moduleId: String): ResourceCleanupResult

    /**
     * Release all resources across all modules. Called on runtime shutdown.
     */
    fun releaseAll(): Map<String, ResourceCleanupResult>

    /** Takes a point-in-time snapshot of all tracked resources. */
    fun takeSnapshot(): ResourceSnapshot

    /** Returns a snapshot for a single module. */
    fun getModuleSnapshot(moduleId: String): ModuleResourceSnapshot

    /** Returns the number of currently active (unreleased) resources across all modules. */
    fun activeCount(): Int

    /** Returns the number of active resources for a specific module. */
    fun activeCountForModule(moduleId: String): Int

    fun getReporter(): cc.arccore.runtime.resource.reporting.ResourceReporter
}
