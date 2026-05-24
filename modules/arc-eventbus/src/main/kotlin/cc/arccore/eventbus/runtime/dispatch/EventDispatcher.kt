package cc.arccore.eventbus.runtime.dispatch

import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.subscription.EventSubscription

/**
 * Abstraction for dispatching an event envelope to a list of subscriptions.
 */
interface EventDispatcher {

    /**
     * Dispatches the event in [envelope] to all [subscriptions].
     *
     * @param envelope The wrapped event with metadata.
     * @param subscriptions The pre-resolved active subscriptions for this event type.
     * @return The result of the dispatch operation.
     */
    fun <T : InternalEvent> dispatch(
        envelope: EventEnvelope<T>,
        subscriptions: List<EventSubscription<T>>
    ): DispatchResult
}
