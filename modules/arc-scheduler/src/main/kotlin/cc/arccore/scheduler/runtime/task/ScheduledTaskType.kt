package cc.arccore.scheduler.runtime.task

enum class ScheduledTaskType {
    SYNC_ONCE,
    ASYNC_ONCE,
    SYNC_DELAYED,
    ASYNC_DELAYED,
    SYNC_REPEATING,
    ASYNC_REPEATING,
    COROUTINE_ONCE,
    COROUTINE_DELAYED,
    COROUTINE_REPEATING;

    val isRepeating: Boolean get() = this == SYNC_REPEATING || this == ASYNC_REPEATING || this == COROUTINE_REPEATING
    val isAsync: Boolean get() = this == ASYNC_ONCE || this == ASYNC_DELAYED || this == ASYNC_REPEATING || this == COROUTINE_ONCE || this == COROUTINE_DELAYED || this == COROUTINE_REPEATING
    val isCoroutine: Boolean get() = this == COROUTINE_ONCE || this == COROUTINE_DELAYED || this == COROUTINE_REPEATING
}
