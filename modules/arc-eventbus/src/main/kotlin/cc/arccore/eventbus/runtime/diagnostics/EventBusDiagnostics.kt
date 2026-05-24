package cc.arccore.eventbus.runtime.diagnostics

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent
import java.util.UUID

/**
 * Diagnostics interface for [InternalEventBus].
 *
 * Provides observability into dispatch traces, subscription counts, and snapshots.
 * Implementations may be no-ops or full-featured depending on the runtime configuration.
 */
interface EventBusDiagnostics {

    /**
     * Records a completed dispatch trace.
     * Called by [DefaultInternalEventBus] after each dispatch.
     */
    fun <T : InternalEvent> recordDispatch(
        envelope: EventEnvelope<T>,
        result: DispatchResult
    )

    /**
     * Returns the trace for a given [dispatchId], or null if not found.
     */
    fun getTrace(dispatchId: UUID): EventDispatchTrace?

    /**
     * Returns the most recent [limit] dispatch traces, newest-first.
     */
    fun recentTraces(limit: Int = 50): List<EventDispatchTrace>

    /**
     * Captures a full snapshot of the current event bus state.
     */
    fun snapshot(): EventBusSnapshot

    /**
     * Clears all stored traces.
     */
    fun clearTraces()
}
