package cc.arccore.runtime.resource.integration

import cc.arccore.runtime.resource.ResourceDescriptor
import cc.arccore.runtime.resource.cleanup.ResourceCleanupResult

/**
 * Callback bridge between ResourceTracker and external leak detection systems.
 * arc-runtime must not import arc-diagnostics, so this bridge uses a function type.
 *
 * Usage in arc-core (ArcCorePluginDelegate):
 *   resourceTracker.setLeakBridge(ResourceLeakBridge(
 *       onOrphanDetected = { desc -> leakDetectionManager.recordLeak(...) },
 *       onCleanupResult = { result -> ... }
 *   ))
 */
class ResourceLeakBridge(
    private val onOrphanDetected: (ResourceDescriptor) -> Unit = {},
    private val onCleanupResult: (ResourceCleanupResult) -> Unit = {},
    private val onAllReleased: (moduleId: String) -> Unit = {}
) {
    fun notifyOrphan(descriptor: ResourceDescriptor) = onOrphanDetected(descriptor)
    fun notifyCleanupResult(result: ResourceCleanupResult) = onCleanupResult(result)
    fun notifyAllReleased(moduleId: String) = onAllReleased(moduleId)
}
