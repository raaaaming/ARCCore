package cc.arccore.runtime.annotation.exception

open class AnnotationScanException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
