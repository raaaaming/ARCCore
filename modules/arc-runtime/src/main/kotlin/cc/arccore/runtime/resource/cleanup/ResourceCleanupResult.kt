package cc.arccore.runtime.resource.cleanup

import cc.arccore.runtime.resource.ResourceDescriptor
import java.time.Instant

data class ResourceCleanupError(
    val descriptor: ResourceDescriptor,
    val cause: Throwable
)

data class ResourceCleanupResult(
    val moduleId: String,
    val releasedCount: Int,
    val alreadyReleasedCount: Int,
    val errors: List<ResourceCleanupError>,
    val completedAt: Instant = Instant.now()
) {
    val isSuccess: Boolean get() = errors.isEmpty()
    val totalAttempted: Int get() = releasedCount + alreadyReleasedCount + errors.size
}
