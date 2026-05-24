package cc.arccore.api.service.exception

class ServiceRegistrationException(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause)
