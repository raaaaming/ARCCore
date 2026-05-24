package cc.arccore.di.lifecycle

import cc.arccore.api.di.DIContainer
import cc.arccore.api.lifecycle.FilteredLifecycleObserver
import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType

class DILifecycleManager(
    private val container: DIContainer
) : FilteredLifecycleObserver(
    LifecycleEventType.UNLOADED,
    LifecycleEventType.FAILED,
    LifecycleEventType.DEPENDENCY_FAILED
) {
    override fun onAcceptedEvent(event: LifecycleEvent) {
        container.clearModule(event.container.module.id)
    }
}
