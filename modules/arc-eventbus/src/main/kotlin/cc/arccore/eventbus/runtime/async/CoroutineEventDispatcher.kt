package cc.arccore.eventbus.runtime.async

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.propagation.PropagationContext
import cc.arccore.eventbus.runtime.propagation.PropagationPipeline
import cc.arccore.eventbus.runtime.subscription.EventSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Coroutine-based event dispatcher.
 *
 * This is the ONLY file in arc-eventbus that imports [kotlinx.coroutines].
 * All other dispatch paths use the lambda injection pattern to avoid
 * a hard compile-time dependency on coroutines.
 *
 * The pipeline is created with a coroutine-native listener executor: each listener is
 * called as a true suspend invocation rather than bridged through [runBlocking].
 *
 * Provides fire-and-forget ([launchDispatch]), awaitable ([asyncDispatch]),
 * and suspend ([suspendDispatch]) variants.
 */
class CoroutineEventDispatcher(
    private val scope: CoroutineScope
) {

    /**
     * A pipeline whose listener executor calls suspend listeners natively within the
     * coroutine context. [runBlocking] bridges the non-suspend [PropagationPipeline.execute]
     * entry point to the suspend listener call.
     */
    private val pipeline: PropagationPipeline = PropagationPipeline(
        listenerExecutor = { block -> runBlocking { block() } }
    )

    /**
     * Launches event dispatch as a fire-and-forget coroutine on [scope].
     *
     * Errors are collected in the [DispatchResult] rather than propagating as exceptions.
     */
    fun <T : InternalEvent> launchDispatch(
        envelope: EventEnvelope<T>,
        subscriptions: List<EventSubscription<T>>
    ) {
        if (subscriptions.isEmpty()) return
        scope.launch {
            val context = PropagationContext(envelope, subscriptions)
            pipeline.execute(context)
        }
    }

    /**
     * Dispatches the event as a coroutine and returns a [Deferred] result.
     *
     * Await the returned [Deferred] to obtain the [DispatchResult].
     */
    fun <T : InternalEvent> asyncDispatch(
        envelope: EventEnvelope<T>,
        subscriptions: List<EventSubscription<T>>
    ): Deferred<DispatchResult> {
        if (subscriptions.isEmpty()) {
            return scope.async { DispatchResult.noListeners(envelope.dispatchId) }
        }
        return scope.async {
            val context = PropagationContext(envelope, subscriptions)
            pipeline.execute(context)
        }
    }

    /**
     * Suspends until event dispatch completes and returns the [DispatchResult].
     */
    suspend fun <T : InternalEvent> suspendDispatch(
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
