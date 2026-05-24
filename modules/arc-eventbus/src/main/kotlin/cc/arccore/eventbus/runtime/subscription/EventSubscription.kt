package cc.arccore.eventbus.runtime.subscription

import cc.arccore.eventbus.runtime.event.EventPriority
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.propagation.PropagationPhase
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * Internal representation of a registered event subscription.
 *
 * @param T The event type this subscription handles.
 * @param subscriptionId Unique identifier for this subscription.
 * @param eventType The KClass of the event type.
 * @param moduleId The owning module's ID.
 * @param phase The propagation phase in which this listener executes.
 * @param priority The execution priority within the phase.
 * @param listener The actual event handler.
 * @param registeredAt The instant this subscription was registered.
 */
class EventSubscription<T : InternalEvent>(
    val subscriptionId: UUID = UUID.randomUUID(),
    val eventType: KClass<T>,
    val moduleId: String,
    val phase: PropagationPhase,
    val priority: EventPriority,
    val listener: InternalEventListener<T>,
    val registeredAt: Instant = Instant.now()
) {
    private val _active = AtomicBoolean(true)

    /** Whether this subscription is currently active and will receive events. */
    val active: Boolean get() = _active.get()

    /**
     * Deactivates this subscription. Idempotent.
     * @return true if this call changed the state from active to inactive, false if already inactive.
     */
    fun deactivate(): Boolean = _active.compareAndSet(true, false)

    override fun toString(): String =
        "EventSubscription(id=$subscriptionId, module=$moduleId, " +
            "event=${eventType.simpleName}, phase=$phase, priority=$priority, active=$active)"
}
