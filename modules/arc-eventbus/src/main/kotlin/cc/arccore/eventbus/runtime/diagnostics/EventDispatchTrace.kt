package cc.arccore.eventbus.runtime.diagnostics

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import java.time.Instant
import java.util.UUID

/**
 * Trace record for a single event dispatch operation.
 *
 * Stored by [DefaultEventBusDiagnostics] with a rolling limit of 1000 entries.
 *
 * @param dispatchId Unique ID of the dispatch (matches [DispatchResult.dispatchId]).
 * @param eventName Human-readable event name.
 * @param eventTypeName Fully qualified class name of the event.
 * @param publisherModuleId Module that published the event, or null for system events.
 * @param dispatchedAt When the dispatch was initiated.
 * @param completedAt When the dispatch completed.
 * @param result The dispatch result.
 * @param async Whether this was an async dispatch.
 */
data class EventDispatchTrace(
    val dispatchId: UUID,
    val eventName: String,
    val eventTypeName: String,
    val publisherModuleId: String?,
    val dispatchedAt: Instant,
    val completedAt: Instant,
    val result: DispatchResult,
    val async: Boolean = false
) {
    val durationMs: Long get() =
        completedAt.toEpochMilli() - dispatchedAt.toEpochMilli()

    val wasSuccessful: Boolean get() = result.isSuccess
    val listenerCount: Int get() = result.invokedCount
    val wasCancelled: Boolean get() = result.wasCancelled
}
