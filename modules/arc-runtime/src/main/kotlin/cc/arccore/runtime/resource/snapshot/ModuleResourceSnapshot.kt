package cc.arccore.runtime.resource.snapshot

import cc.arccore.runtime.resource.ResourceDescriptor
import cc.arccore.runtime.resource.ResourceState
import cc.arccore.runtime.resource.ResourceType
import java.time.Instant

data class ModuleResourceSnapshot(
    val moduleId: String,
    val snapshotAt: Instant,
    val resources: List<ResourceDescriptor>
) {
    val activeCount: Int get() = resources.count { it.state == ResourceState.ACTIVE || it.state == ResourceState.CREATED }
    val releasedCount: Int get() = resources.count { it.isReleased }
    val totalCount: Int get() = resources.size

    fun byType(): Map<ResourceType, List<ResourceDescriptor>> = resources.groupBy { it.type }

    fun activeByType(): Map<ResourceType, Int> =
        resources.filter { !it.isReleased }.groupBy { it.type }.mapValues { it.value.size }
}
