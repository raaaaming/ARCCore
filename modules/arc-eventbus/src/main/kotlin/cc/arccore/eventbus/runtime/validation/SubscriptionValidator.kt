package cc.arccore.eventbus.runtime.validation

import cc.arccore.eventbus.runtime.event.EventPriority
import cc.arccore.eventbus.runtime.event.InternalEvent
import cc.arccore.eventbus.runtime.propagation.PropagationPhase
import cc.arccore.eventbus.runtime.subscription.InternalEventListener
import kotlin.reflect.KClass

/**
 * Validates subscription parameters before registration.
 *
 * Called by [DefaultInternalEventBus.subscribe] to enforce constraints.
 */
object SubscriptionValidator {

    /**
     * Validates all parameters for a subscription registration.
     *
     * @param eventType The event type to subscribe to.
     * @param moduleId The owning module's ID.
     * @param phase The propagation phase.
     * @param priority The listener priority.
     * @param listener The event listener.
     * @return [ValidationResult.Valid] or [ValidationResult.Invalid] with reasons.
     */
    fun <T : InternalEvent> validate(
        eventType: KClass<T>,
        moduleId: String,
        phase: PropagationPhase,
        priority: EventPriority,
        listener: InternalEventListener<T>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate moduleId
        if (moduleId.isBlank()) {
            errors += "moduleId must not be blank"
        }

        if (moduleId.length > 256) {
            errors += "moduleId exceeds maximum length of 256 characters"
        }

        // Validate MONITOR phase restrictions
        if (phase == PropagationPhase.MONITOR && priority != EventPriority.MONITOR) {
            // Allow but warn — MONITOR phase accepts any priority, but MONITOR priority is conventional
        }

        // Validate event type
        val typeResult = EventTypeValidator.validate(eventType)
        if (!typeResult.isValid) {
            errors += typeResult.errors
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }
}
