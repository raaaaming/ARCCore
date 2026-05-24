package cc.arccore.runtime.resource

import java.time.Instant
import java.util.UUID

/**
 * Rich metadata for a single tracked runtime resource.
 * Immutable except for [state], which transitions through the resource lifecycle.
 *
 * Why UUID id:
 *   Modules may register multiple resources of the same type (e.g., several executors).
 *   UUID guarantees uniqueness without requiring the caller to invent unique names.
 */
data class ResourceDescriptor(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: ResourceType,
    val owner: ResourceOwner,
    @Volatile var state: ResourceState = ResourceState.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap()
) {
    val moduleId: String get() = owner.moduleId
    val isReleased: Boolean get() = state == ResourceState.RELEASED || state == ResourceState.VERIFIED
}
