package cc.arccore.event.lifecycle

import cc.arccore.api.lifecycle.FilteredLifecycleObserver
import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.event.listener.ListenerRegistry

class ListenerLifecycleManager(
    private val registry: ListenerRegistry
) : FilteredLifecycleObserver(
    LifecycleEventType.UNLOADED,
    LifecycleEventType.FAILED,
    LifecycleEventType.DEPENDENCY_FAILED
) {

    override fun onAcceptedEvent(event: LifecycleEvent) {
        registry.unregisterAll(event.container)
    }
}
