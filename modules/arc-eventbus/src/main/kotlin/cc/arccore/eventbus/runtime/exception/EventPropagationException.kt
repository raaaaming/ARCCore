package cc.arccore.eventbus.runtime.exception

/**
 * Thrown when illegal propagation control is attempted.
 *
 * For example, calling [CancellableInternalEvent.cancel] from a MONITOR phase listener
 * is illegal — MONITOR phase is read-only and must not alter propagation state.
 */
class EventPropagationException(
    message: String,
    cause: Throwable? = null
) : EventBusException(message, cause)
