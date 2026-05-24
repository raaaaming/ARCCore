package cc.arccore.eventbus.runtime.diagnostics

import cc.arccore.eventbus.runtime.async.AsyncEventQueue
import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.ownership.OwnershipRegistry
import cc.arccore.eventbus.runtime.subscription.SubscriptionRegistry
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of [EventBusDiagnostics].
 *
 * Stores up to [traceLimit] traces in a rolling [ConcurrentHashMap].
 * When the limit is reached, the oldest entry is evicted before adding a new one.
 *
 * @param registry The subscription registry to query for snapshot data.
 * @param ownership The ownership registry to query for per-module subscription counts.
 * @param busIsShutdown Lambda that returns whether the bus has been shut down.
 * @param totalDispatched Lambda that returns the total dispatch count.
 * @param totalFailed Lambda that returns the total failure count.
 * @param asyncQueue Optional async event queue for pending event count.
 * @param traceLimit Maximum number of traces to retain (default 1000).
 */
class DefaultEventBusDiagnostics(
    private val registry: SubscriptionRegistry,
    private val ownership: OwnershipRegistry,
    private val busIsShutdown: () -> Boolean,
    private val totalDispatched: () -> Long,
    private val totalFailed: () -> Long,
    private val asyncQueue: AsyncEventQueue? = null,
    private val traceLimit: Int = 1000
) : EventBusDiagnostics {

    // Ordered insertion tracking
    private val traceMap = ConcurrentHashMap<UUID, EventDispatchTrace>(traceLimit)
    private val insertionOrder = ArrayDeque<UUID>(traceLimit)
    private val lock = Any()

    override fun <T : InternalEvent> recordDispatch(
        envelope: EventEnvelope<T>,
        result: DispatchResult
    ) {
        val trace = EventDispatchTrace(
            dispatchId = envelope.dispatchId,
            eventName = envelope.eventName,
            eventTypeName = envelope.event::class.qualifiedName ?: envelope.event::class.simpleName ?: "Unknown",
            publisherModuleId = envelope.publisherModuleId,
            dispatchedAt = envelope.dispatchedAt,
            completedAt = Instant.now(),
            result = result,
            async = envelope.async
        )

        synchronized(lock) {
            if (traceMap.size >= traceLimit && insertionOrder.isNotEmpty()) {
                val oldest = insertionOrder.removeFirst()
                traceMap.remove(oldest)
            }
            traceMap[trace.dispatchId] = trace
            insertionOrder.addLast(trace.dispatchId)
        }
    }

    override fun getTrace(dispatchId: UUID): EventDispatchTrace? =
        traceMap[dispatchId]

    override fun recentTraces(limit: Int): List<EventDispatchTrace> {
        synchronized(lock) {
            return insertionOrder
                .takeLast(minOf(limit, insertionOrder.size))
                .reversed()
                .mapNotNull { traceMap[it] }
        }
    }

    override fun snapshot(): EventBusSnapshot {
        val allSubs = registry.allSubscriptions()
        val activeSubs = allSubs.filter { it.active }

        val subscriptionsByModule = activeSubs
            .groupBy { it.moduleId }
            .mapValues { (_, list) -> list.size }

        return EventBusSnapshot(
            capturedAt = Instant.now(),
            isShutdown = busIsShutdown(),
            totalSubscriptions = allSubs.size,
            activeSubscriptions = activeSubs.size,
            registeredEventTypes = registry.eventTypeCount(),
            registeredModules = ownership.registeredModules(),
            totalDispatched = totalDispatched(),
            totalFailed = totalFailed(),
            asyncQueueSize = asyncQueue?.size ?: 0,
            recentTraces = recentTraces(50),
            subscriptionsByModule = subscriptionsByModule
        )
    }

    override fun clearTraces() {
        synchronized(lock) {
            traceMap.clear()
            insertionOrder.clear()
        }
    }
}
