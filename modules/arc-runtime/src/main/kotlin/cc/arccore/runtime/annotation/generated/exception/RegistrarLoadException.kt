package cc.arccore.runtime.annotation.generated.exception

open class GeneratedRegistrarException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class RegistrarLoadException(message: String, cause: Throwable? = null) : GeneratedRegistrarException(message, cause)
