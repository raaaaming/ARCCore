package cc.arccore.runtime.context.async

interface AsyncRuntimeAccess {
    fun capabilities(): AsyncCapability
    fun close() {} // 기본 no-op; CoroutineRuntime은 이를 오버라이드해 scope.cancel() 호출
    fun activeTaskCount(): Int = 0  // override in CoroutineRuntime
}

enum class AsyncCapability {
    NONE, THREAD_POOL, COROUTINE, DISTRIBUTED
}

object NoOpAsyncRuntimeAccess : AsyncRuntimeAccess {
    override fun capabilities() = AsyncCapability.NONE
}
