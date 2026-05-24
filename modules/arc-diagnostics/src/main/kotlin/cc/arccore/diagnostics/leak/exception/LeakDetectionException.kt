package cc.arccore.diagnostics.leak.exception

open class LeakDetectionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class RuntimeIntegrityException(message: String, cause: Throwable? = null) : LeakDetectionException(message, cause)

class ClassLoaderLeakException(
    val moduleId: String,
    message: String,
    cause: Throwable? = null
) : LeakDetectionException(message, cause)
