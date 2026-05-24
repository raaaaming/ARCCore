package cc.arccore.event.exception

class InvalidListenerException(
    message: String,
    cause: Throwable? = null
) : ListenerRegistrationException(message, cause)
