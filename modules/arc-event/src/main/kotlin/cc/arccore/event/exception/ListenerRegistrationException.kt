package cc.arccore.event.exception

open class ListenerRegistrationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
