package cc.arccore.eventbus.runtime.event

import cc.arccore.eventbus.runtime.exception.EventPropagationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [InternalEvent] that supports cancellation.
 *
 * When cancelled, propagation stops after the current phase completes —
 * except for MONITOR phase listeners, which always receive the event regardless
 * of cancellation state.
 *
 * **MONITOR phase restriction:** Calling [cancel] from a MONITOR phase listener
 * will throw [EventPropagationException]. MONITOR listeners are read-only observers.
 */
abstract class CancellableInternalEvent : InternalEvent {

    private val cancelled = AtomicBoolean(false)

    /**
     * Whether this event has been cancelled.
     *
     * Cancelled events still propagate to MONITOR phase listeners.
     */
    val isCancelled: Boolean get() = cancelled.get()

    /**
     * Cancels this event.
     *
     * Must not be called from a MONITOR phase listener.
     * The [PropagationPipeline] enforces this restriction and will throw
     * [EventPropagationException] if violated.
     *
     * This method is idempotent — calling it multiple times has no additional effect.
     */
    fun cancel() {
        cancelled.set(true)
    }

    /**
     * Uncancels this event (re-enables propagation).
     *
     * Must not be called from a MONITOR phase listener.
     */
    fun uncancel() {
        cancelled.set(false)
    }
}
