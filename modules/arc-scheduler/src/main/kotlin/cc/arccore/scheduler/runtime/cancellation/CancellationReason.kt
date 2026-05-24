package cc.arccore.scheduler.runtime.cancellation

enum class CancellationReason {
    MANUAL,
    MODULE_UNLOAD,
    MODULE_RELOAD,
    TIMEOUT,
    EXCEPTION,
    SCHEDULER_CLOSE
}
