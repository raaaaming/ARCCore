package cc.arccore.runtime.resource

/**
 * Lifecycle states for a tracked resource.
 *
 *   CREATED   — registered in the tracker, not yet confirmed active
 *   ACTIVE    — in use by the module
 *   CLEANING  — cleanup initiated (module unloading)
 *   RELEASED  — cleanup completed, no longer reachable
 *   VERIFIED  — post-unload verification confirms classloader was freed
 */
enum class ResourceState {
    CREATED,
    ACTIVE,
    CLEANING,
    RELEASED,
    VERIFIED;

    fun canTransitionTo(next: ResourceState): Boolean = when (this) {
        CREATED -> next == ACTIVE || next == CLEANING || next == RELEASED
        ACTIVE -> next == CLEANING || next == RELEASED
        CLEANING -> next == RELEASED
        RELEASED -> next == VERIFIED
        VERIFIED -> false
    }
}
