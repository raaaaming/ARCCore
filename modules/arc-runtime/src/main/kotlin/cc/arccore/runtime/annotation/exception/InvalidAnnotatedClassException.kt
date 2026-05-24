package cc.arccore.runtime.annotation.exception

class InvalidAnnotatedClassException(
    message: String,
    cause: Throwable? = null
) : AnnotationScanException(message, cause)
