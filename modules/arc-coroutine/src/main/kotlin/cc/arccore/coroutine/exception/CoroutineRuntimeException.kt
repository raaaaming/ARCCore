package cc.arccore.coroutine.exception

open class CoroutineRuntimeException(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause)

class InvalidCoroutineScopeException(val moduleId: String, reason: String)
    : CoroutineRuntimeException("Module '$moduleId' coroutine scope is invalid: $reason")

class AsyncLifecycleException(val moduleId: String, val phase: String, cause: Throwable? = null)
    : CoroutineRuntimeException("Async lifecycle error in module '$moduleId' at phase '$phase'", cause)

class DispatcherUnavailableException(val dispatcher: String)
    : CoroutineRuntimeException("Dispatcher '$dispatcher' is unavailable or has been shut down")
