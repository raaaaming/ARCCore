package cc.arccore.eventbus.runtime.exception

/**
 * Base exception for all event bus errors.
 */
open class EventBusException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
