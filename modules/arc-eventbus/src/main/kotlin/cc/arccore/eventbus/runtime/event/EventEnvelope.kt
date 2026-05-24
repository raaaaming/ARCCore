package cc.arccore.eventbus.runtime.event

import java.time.Instant
import java.util.UUID

/**
 * Wraps an [InternalEvent] with dispatch metadata.
 *
 * Created by the [DefaultInternalEventBus] at dispatch time and passed through
 * the propagation pipeline.
 *
 * @param T The concrete event type.
 * @param event The event being dispatched.
 * @param publisherModuleId The module ID that published the event, or null for system events.
 * @param dispatchId Unique identifier for this dispatch operation (useful for trace correlation).
 * @param dispatchedAt The instant this envelope was created.
 * @param async Whether this dispatch was initiated asynchronously.
 */
data class EventEnvelope<T : InternalEvent>(
    val event: T,
    val publisherModuleId: String?,
    val dispatchId: UUID = UUID.randomUUID(),
    val dispatchedAt: Instant = Instant.now(),
    val async: Boolean = false
) {
    val eventType: Class<out T> get() = event.javaClass
    val eventName: String get() = event.eventName
}
