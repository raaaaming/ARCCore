package cc.arccore.eventbus.runtime.dispatch

import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.propagation.PropagationContext
import cc.arccore.eventbus.runtime.propagation.PropagationPipeline
import cc.arccore.eventbus.runtime.subscription.EventSubscription
import kotlinx.coroutines.runBlocking

/**
 * Synchronous event dispatcher.
 *
 * Executes the [PropagationPipeline] on the calling thread, bridging suspend listeners
 * via [runBlocking]. This dispatcher blocks the calling thread until all listeners complete.
 *
 * The pipeline is constructed with a [runBlocking]-based [listenerExecutor] so that
 * [PropagationPipeline] itself remains free of coroutine imports.
 */
class SyncEventDispatcher : EventDispatcher {

    /**
     * Pipeline with a runBlocking-based listener executor.
     * suspend listeners are bridged to blocking execution on the caller's thread.
     */
    private val pipeline: PropagationPipeline = PropagationPipeline(
        listenerExecutor = { block -> runBlocking { block() } }
    )

    override fun <T : InternalEvent> dispatch(
        envelope: EventEnvelope<T>,
        subscriptions: List<EventSubscription<T>>
    ): DispatchResult {
        if (subscriptions.isEmpty()) {
            return DispatchResult.noListeners(envelope.dispatchId)
        }
        val context = PropagationContext(envelope, subscriptions)
        return pipeline.execute(context)
    }
}
