package cc.arccore.runtime.resource.registration

import cc.arccore.runtime.resource.ResourceDescriptor
import cc.arccore.runtime.resource.ResourceHandle

internal data class ResourceRegistration(
    val descriptor: ResourceDescriptor,
    val handle: ResourceHandle,
    val cleanupFn: () -> Unit
)
