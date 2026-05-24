package cc.arccore.ksp.exception

open class GeneratedRegistrarException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class BootstrapGenerationException(message: String, cause: Throwable? = null) : GeneratedRegistrarException(message, cause)
