package cc.arccore.scheduler.runtime.exception

open class SchedulerRuntimeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class TaskCancellationException(
    val taskId: String,
    val reason: String,
    cause: Throwable? = null
) : SchedulerRuntimeException("Task '$taskId' was cancelled: $reason", cause)

class InvalidTaskStateException(
    val taskId: String,
    val currentStatus: String,
    message: String
) : SchedulerRuntimeException(message)

class SchedulingException(
    val moduleId: String,
    message: String,
    cause: Throwable? = null
) : SchedulerRuntimeException(message, cause)

class ModuleUnloadedException(
    val moduleId: String
) : SchedulerRuntimeException("Cannot schedule task: module '$moduleId' is unloaded or not found")
