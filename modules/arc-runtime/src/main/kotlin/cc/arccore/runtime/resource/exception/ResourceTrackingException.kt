package cc.arccore.runtime.resource.exception

open class ResourceTrackingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidResourceStateException(
    val resourceId: String,
    val currentState: String,
    val attemptedOperation: String,
    message: String = "Cannot $attemptedOperation resource '$resourceId' in state $currentState"
) : ResourceTrackingException(message)

class ResourceOwnershipException(
    val resourceId: String,
    message: String,
    cause: Throwable? = null
) : ResourceTrackingException(message, cause)

class ResourceCleanupException(
    val moduleId: String,
    message: String,
    cause: Throwable? = null
) : ResourceTrackingException(message, cause)
