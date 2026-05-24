package cc.arccore.eventbus.runtime.async

import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * A thread-safe queue for pending asynchronous event dispatches.
 *
 * Events are enqueued when the async dispatch strategy schedules them,
 * and dequeued when the strategy executes.
 *
 * This queue is primarily used for backpressure monitoring and diagnostics;
 * the actual execution is delegated to the injected [asyncDispatchStrategy].
 */
class AsyncEventQueue {

    private val queue = ConcurrentLinkedQueue<QueuedEvent<*>>()
    private val _size = AtomicInteger(0)

    /** Current number of events pending in the queue. */
    val size: Int get() = _size.get()

    /**
     * Enqueues an event for async dispatch.
     *
     * @param envelope The event envelope to queue.
     * @return true if successfully enqueued.
     */
    fun <T : InternalEvent> enqueue(envelope: EventEnvelope<T>): Boolean {
        queue.offer(QueuedEvent(envelope))
        _size.incrementAndGet()
        return true
    }

    /**
     * Dequeues the next event, or null if the queue is empty.
     */
    fun dequeue(): QueuedEvent<*>? {
        val event = queue.poll() ?: return null
        _size.decrementAndGet()
        return event
    }

    /**
     * Returns true if the queue is empty.
     */
    fun isEmpty(): Boolean = queue.isEmpty()

    /**
     * Clears all pending events.
     * @return The number of events that were cleared.
     */
    fun clear(): Int {
        var count = 0
        while (queue.poll() != null) {
            count++
            _size.decrementAndGet()
        }
        return count
    }

    /**
     * Returns a snapshot of all queued events (for diagnostics).
     */
    fun snapshot(): List<QueuedEvent<*>> = queue.toList()
}

/**
 * A wrapper for an enqueued event with its envelope.
 */
data class QueuedEvent<T : InternalEvent>(
    val envelope: EventEnvelope<T>,
    val enqueuedAt: Long = System.currentTimeMillis()
)
