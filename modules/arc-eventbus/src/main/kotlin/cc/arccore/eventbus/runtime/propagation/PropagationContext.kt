package cc.arccore.eventbus.runtime.propagation

import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.subscription.EventSubscription

/**
 * Mutable context for a single event propagation run.
 *
 * Carries the [envelope], the resolved [subscriptions], and mutable state
 * such as [stopped] and [errors] that are updated as propagation proceeds.
 *
 * @param T The concrete event type.
 */
class PropagationContext<T : InternalEvent>(
    val envelope: EventEnvelope<T>,
    val subscriptions: List<EventSubscription<T>>
) {

    /**
     * When true, propagation has been stopped (e.g., by a cancelled [CancellableInternalEvent]).
     * MONITOR phase always executes regardless of this flag.
     */
    var stopped: Boolean = false

    /**
     * Accumulated (moduleId, exception) pairs from listener errors during propagation.
     * A non-empty list does NOT prevent other listeners from executing.
     */
    val errors: MutableList<Pair<String, Throwable>> = mutableListOf()

    /** The current phase being executed. */
    var currentPhase: PropagationPhase = PropagationPhase.PRE_PROCESS

    /** Total number of listeners invoked (including those that threw exceptions). */
    var invokedCount: Int = 0

    /** Total number of listeners that succeeded without exception. */
    var successCount: Int = 0
}
