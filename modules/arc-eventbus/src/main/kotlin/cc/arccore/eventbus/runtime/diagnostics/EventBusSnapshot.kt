package cc.arccore.eventbus.runtime.diagnostics

import java.time.Instant

/**
 * Point-in-time snapshot of [InternalEventBus] state for observability.
 *
 * @param capturedAt When this snapshot was taken.
 * @param isShutdown Whether the bus has been shut down.
 * @param totalSubscriptions Total number of registered subscriptions (including inactive).
 * @param activeSubscriptions Number of currently active subscriptions.
 * @param registeredEventTypes Number of distinct event types with at least one subscription.
 * @param registeredModules Set of module IDs that have at least one subscription.
 * @param totalDispatched Total number of events dispatched since bus creation.
 * @param totalFailed Total number of dispatch operations that produced at least one error.
 * @param asyncQueueSize Current number of events pending in the async queue (0 if no async strategy).
 * @param recentTraces The most recent dispatch traces (up to the trace limit).
 * @param subscriptionsByModule Map of moduleId to number of active subscriptions.
 */
data class EventBusSnapshot(
    val capturedAt: Instant,
    val isShutdown: Boolean,
    val totalSubscriptions: Int,
    val activeSubscriptions: Int,
    val registeredEventTypes: Int,
    val registeredModules: Set<String>,
    val totalDispatched: Long,
    val totalFailed: Long,
    val asyncQueueSize: Int,
    val recentTraces: List<EventDispatchTrace>,
    val subscriptionsByModule: Map<String, Int>
)
