package cc.arccore.eventbus.runtime

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventPriority
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.propagation.PropagationPhase
import cc.arccore.eventbus.runtime.subscription.InternalEventListener
import cc.arccore.eventbus.runtime.subscription.SubscriptionHandle
import kotlin.reflect.KClass

/**
 * Core event bus for internal framework and module-to-module communication.
 *
 * Events dispatched through this bus are [InternalEvent] instances — they are
 * NOT Bukkit/Paper events and must not extend Bukkit's event system.
 *
 * ## Lifecycle
 * Once [shutdown] is called, all subsequent [subscribe] and [publish] calls will throw
 * [cc.arccore.eventbus.runtime.exception.EventBusException].
 *
 * ## Propagation
 * Events propagate through phases (PRE_PROCESS → NORMAL → POST_PROCESS → MONITOR)
 * and within each phase in priority order (LOW → NORMAL → HIGH → MONITOR).
 *
 * ## Module cleanup
 * Use [unsubscribeAll] (or [ModuleEventCleanup][cc.arccore.eventbus.runtime.lifecycle.ModuleEventCleanup])
 * to cancel all subscriptions owned by a module when it unloads.
 *
 * ## Coroutine integration
 * Async dispatch is opt-in via the `asyncDispatchStrategy` constructor parameter on
 * [DefaultInternalEventBus]. Without it, [publishAsync] throws an exception.
 */
interface InternalEventBus {

    /**
     * Subscribes [listener] to events of type [T] under the given [moduleId].
     *
     * @param eventType The KClass of the event to subscribe to.
     * @param moduleId The owning module's identifier (used for cleanup and ownership tracking).
     * @param phase The propagation phase. Defaults to [PropagationPhase.NORMAL].
     * @param priority The execution priority within the phase. Defaults to [EventPriority.NORMAL].
     * @param listener The handler to invoke when an event of type [T] is dispatched.
     * @return A [SubscriptionHandle] that can be used to cancel this subscription.
     * @throws cc.arccore.eventbus.runtime.exception.EventBusException if the bus is shut down.
     * @throws cc.arccore.eventbus.runtime.exception.InvalidSubscriptionException if parameters are invalid.
     */
    fun <T : InternalEvent> subscribe(
        eventType: KClass<T>,
        moduleId: String,
        phase: PropagationPhase = PropagationPhase.NORMAL,
        priority: EventPriority = EventPriority.NORMAL,
        listener: InternalEventListener<T>
    ): SubscriptionHandle

    /**
     * Synchronously dispatches [event] to all active subscribers.
     *
     * Blocks the calling thread until all listeners have been invoked.
     *
     * @param event The event to dispatch.
     * @return A [DispatchResult] summarising the dispatch outcome.
     * @throws cc.arccore.eventbus.runtime.exception.EventBusException if the bus is shut down.
     */
    fun <T : InternalEvent> publish(event: T): DispatchResult

    /**
     * Asynchronously dispatches [event] to all active subscribers.
     *
     * Requires that an `asyncDispatchStrategy` was provided at bus construction time.
     *
     * @param event The event to dispatch.
     * @return A [DispatchResult] summarising the dispatch outcome.
     * @throws cc.arccore.eventbus.runtime.exception.EventBusException if the bus is shut down
     *   or no async strategy is configured.
     */
    suspend fun <T : InternalEvent> publishAsync(event: T): DispatchResult

    /**
     * Cancels all subscriptions owned by [moduleId].
     *
     * @return The number of subscriptions cancelled.
     * @throws cc.arccore.eventbus.runtime.exception.EventBusException if the bus is shut down.
     */
    fun unsubscribeAll(moduleId: String): Int

    /**
     * Returns true if [shutdown] has been called.
     */
    fun isShutdown(): Boolean

    /**
     * Shuts down the event bus.
     *
     * After shutdown, all subsequent [subscribe] and [publish] calls throw
     * [cc.arccore.eventbus.runtime.exception.EventBusException].
     * Idempotent — calling this multiple times has no additional effect.
     */
    fun shutdown()
}

// ─── Convenience extension functions ───────────────────────────────────────────

/**
 * Subscribes [listener] to events of type [T] using a reified type parameter.
 *
 * ```kotlin
 * val handle = eventBus.subscribe<ModuleLoadedEvent>(moduleId) { event ->
 *     logger.info("Module loaded: ${event.moduleId}")
 * }
 * ```
 */
inline fun <reified T : InternalEvent> InternalEventBus.subscribe(
    moduleId: String,
    phase: PropagationPhase = PropagationPhase.NORMAL,
    priority: EventPriority = EventPriority.NORMAL,
    listener: InternalEventListener<T>
): SubscriptionHandle = subscribe(T::class, moduleId, phase, priority, listener)

/**
 * Publishes [event] and discards the result.
 */
fun <T : InternalEvent> InternalEventBus.fire(event: T) {
    publish(event)
}

/**
 * Publishes [event] and returns true if dispatched without errors.
 */
fun <T : InternalEvent> InternalEventBus.publishAndCheck(event: T): Boolean =
    publish(event).isSuccess
