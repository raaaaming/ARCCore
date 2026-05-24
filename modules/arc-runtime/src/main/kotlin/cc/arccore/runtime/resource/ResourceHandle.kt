package cc.arccore.runtime.resource

/**
 * A handle returned when a resource is registered with [ResourceTracker].
 * Callers hold this handle to release the resource manually or let the runtime release it automatically.
 *
 * Why handle-based:
 *   Returning a handle decouples tracking from cleanup. The module can call release() early
 *   (e.g., service replaced mid-lifecycle). The runtime calls releaseModule() on unload,
 *   which releases all remaining handles — ensuring no resource is orphaned regardless of
 *   whether the module remembered to release it manually.
 */
interface ResourceHandle {
    val descriptor: ResourceDescriptor

    fun release()
    fun isReleased(): Boolean

    fun moduleId(): String = descriptor.moduleId
    fun resourceType(): ResourceType = descriptor.type
}

internal class DefaultResourceHandle(
    override val descriptor: ResourceDescriptor,
    private val cleanupFn: () -> Unit,
    private val onReleased: (ResourceDescriptor) -> Unit
) : ResourceHandle {

    override fun release() {
        if (descriptor.isReleased) return
        descriptor.state = ResourceState.CLEANING
        try {
            cleanupFn()
        } finally {
            descriptor.state = ResourceState.RELEASED
            onReleased(descriptor)
        }
    }

    override fun isReleased(): Boolean = descriptor.isReleased
}
