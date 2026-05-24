package cc.arccore.coroutine

import cc.arccore.coroutine.dispatcher.ModuleDispatcher
import cc.arccore.runtime.context.BaseRuntimeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

// BaseRuntimeModule을 확장해 coroutines 접근자 추가.
// CoroutineRuntimeFactory가 설정된 환경(ArcCorePlugin)에서만 사용 가능.
abstract class BaseCoroutineModule : BaseRuntimeModule() {

    protected val coroutines: CoroutineRuntime
        get() = runtimeContext.asyncRuntime as? CoroutineRuntime
            ?: error(
                "Module '$id' context does not have a CoroutineRuntime. " +
                    "Ensure CoroutineRuntimeFactory is configured in ModuleRuntime."
            )

    protected fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job =
        coroutines.launch(ModuleDispatcher.Async, block)

    protected fun launchSync(block: suspend CoroutineScope.() -> Unit): Job =
        coroutines.launch(ModuleDispatcher.Sync, block)

    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
        coroutines.launch(ModuleDispatcher.IO, block)

    protected fun <T> asyncDeferred(block: suspend CoroutineScope.() -> T): Deferred<T> =
        coroutines.async(ModuleDispatcher.Async, block)
}
