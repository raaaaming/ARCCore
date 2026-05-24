package cc.arccore.runtime.annotation.exception

class AutoRegistrationException(
    message: String,
    cause: Throwable? = null
) : AnnotationScanException(message, cause)
