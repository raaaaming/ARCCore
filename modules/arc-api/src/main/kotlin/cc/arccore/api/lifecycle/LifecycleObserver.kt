package cc.arccore.api.lifecycle

fun interface LifecycleObserver {
    fun onLifecycleEvent(event: LifecycleEvent)
}

abstract class FilteredLifecycleObserver(
    private val acceptedTypes: Set<LifecycleEventType>
) : LifecycleObserver {

    constructor(vararg types: LifecycleEventType) : this(types.toSet())

    final override fun onLifecycleEvent(event: LifecycleEvent) {
        if (event.type in acceptedTypes) onAcceptedEvent(event)
    }

    protected abstract fun onAcceptedEvent(event: LifecycleEvent)
}
