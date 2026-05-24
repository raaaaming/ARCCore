package cc.arccore.runtime.context

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.resource.ResourceTracker
import cc.arccore.runtime.unload.ModuleResourceTracker
import cc.arccore.runtime.unload.ModuleTaskTracker

interface RuntimeFacade {
    val moduleId: String
    val container: ModuleContainerView
    fun currentState(): ModuleState
    fun trackedTaskCount(): Int
    fun trackedResourceKeys(): Set<String>
    fun resources(): ResourceTracker
}

class DefaultRuntimeFacade(
    override val container: ModuleContainerView,
    private val taskTracker: ModuleTaskTracker,
    private val resourceTracker: ModuleResourceTracker,
    private val ownershipTracker: ResourceTracker
) : RuntimeFacade {
    override val moduleId: String get() = container.module.id
    override fun currentState(): ModuleState = container.state
    override fun trackedTaskCount(): Int = taskTracker.getTrackedTaskCount()
    override fun trackedResourceKeys(): Set<String> = resourceTracker.getTrackedKeys()
    override fun resources(): ResourceTracker = ownershipTracker
}
