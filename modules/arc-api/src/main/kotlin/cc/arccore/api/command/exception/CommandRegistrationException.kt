package cc.arccore.api.command.exception

open class CommandRegistrationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
