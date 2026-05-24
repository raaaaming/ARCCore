package cc.arccore.eventbus.runtime.validation

import cc.arccore.eventbus.runtime.event.InternalEvent
import kotlin.reflect.KClass

/**
 * Validates that a given [KClass] is a valid [InternalEvent] type for subscription.
 *
 * Checks:
 * - The class is not abstract (unless it is an interface or abstract class used as a supertype).
 * - The class is not [InternalEvent] itself (subscribing to the base interface is too broad).
 * - The class does not have a problematic class name.
 */
object EventTypeValidator {

    /**
     * Validates the event type [eventType].
     *
     * @param eventType The KClass to validate.
     * @return [ValidationResult.Valid] or [ValidationResult.Invalid] with reasons.
     */
    fun <T : InternalEvent> validate(eventType: KClass<T>): ValidationResult {
        val errors = mutableListOf<String>()

        if (eventType == InternalEvent::class) {
            errors += "Cannot subscribe to InternalEvent base interface directly. " +
                "Use a specific event subtype."
        }

        val javaClass = eventType.java
        if (javaClass.isAnonymousClass) {
            errors += "Anonymous classes cannot be used as event types: ${eventType.qualifiedName}"
        }

        if (javaClass.isLocalClass) {
            errors += "Local classes cannot be used as event types: ${eventType.qualifiedName}"
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }
}
