package cc.arccore.eventbus.runtime.exception

/**
 * Thrown when event dispatch fails due to listener error or dispatch strategy failure.
 *
 * @param eventType The simple name of the event class that failed to dispatch.
 * @param cause The underlying exception from the listener or dispatcher.
 */
class EventDispatchException(
    val eventType: String,
    message: String,
    cause: Throwable? = null
) : EventBusException(message, cause) {

    constructor(eventType: String, cause: Throwable) : this(
        eventType,
        "Dispatch failed for event '$eventType': ${cause.message}",
        cause
    )
}
