package cc.arccore.eventbus.runtime.dispatch

import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.exception.EventBusException
import cc.arccore.eventbus.runtime.propagation.PropagationContext
import cc.arccore.eventbus.runtime.propagation.PropagationPipeline
import cc.arccore.eventbus.runtime.subscription.EventSubscription

/**
 * Asynchronous event dispatcher.
 *
 * Requires an [asyncDispatchStrategy] to schedule the dispatch coroutine.
 * The strategy is injected from the outside (typically from arc-coroutine) to
 * avoid a compile-time dependency on coroutines in the core dispatch path.
 *
 * The [asyncDispatchStrategy] is used as the [PropagationPipeline] listener executor,
 * so each listener invocation runs through the coroutine context provided by the strategy.
 * The pipeline itself is executed synchronously on the caller's suspend context, which
 * ensures the [DispatchResult] is always available when [dispatch] returns.
 *
 * @param asyncDispatchStrategy A lambda that accepts a suspend block and executes it.
 *   Typically a `launch { block() }` wrapper from arc-coroutine.
 *   If null, [dispatch] will throw [EventBusException].
 */
class AsyncEventDispatcher(
    private val asyncDispatchStrategy: ((suspend () -> Unit) -> Unit)?
) {

    /**
     * Dispatches the event using the [asyncDispatchStrategy] as the listener executor.
     *
     * The [PropagationPipeline] is executed on the calling coroutine; individual listener
     * invocations are handed off to [asyncDispatchStrategy]. This guarantees that
     * [DispatchResult] is always captured before the function returns.
     *
     * @throws EventBusException if no [asyncDispatchStrategy] has been configured.
     */
    suspend fun <T : InternalEvent> dispatch(
        envelope: EventEnvelope<T>,
        subscriptions: List<EventSubscription<T>>
    ): DispatchResult {
        val strategy = asyncDispatchStrategy
            ?: throw EventBusException(
                "Async dispatch requires a coroutine strategy. " +
                    "Provide asyncDispatchStrategy when constructing DefaultInternalEventBus."
            )

        if (subscriptions.isEmpty()) {
            return DispatchResult.noListeners(envelope.dispatchId)
        }

        // The pipeline uses the injected strategy as the listener executor.
        // PropagationPipeline.execute() is called directly on the current coroutine,
        // so the result is captured before returning — no race condition.
        val pipeline = PropagationPipeline(listenerExecutor = strategy)
        val context = PropagationContext(envelope, subscriptions)
        return pipeline.execute(context)
    }
}
