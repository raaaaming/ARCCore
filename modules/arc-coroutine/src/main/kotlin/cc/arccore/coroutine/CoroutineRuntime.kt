package cc.arccore.coroutine

import cc.arccore.coroutine.dispatcher.ModuleDispatcher
import cc.arccore.runtime.context.async.AsyncCapability
import cc.arccore.runtime.context.async.AsyncRuntimeAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

interface CoroutineRuntime : AsyncRuntimeAccess {
    val moduleId: String
    val moduleScope: ModuleCoroutineScope

    // 직접 CoroutineScope 접근 (launch/async/withContext 등)
    val scope: CoroutineScope get() = moduleScope.scope

    // 편의 메서드: 지정 dispatcher로 코루틴 실행
    fun launch(
        dispatcher: ModuleDispatcher = ModuleDispatcher.Async,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    fun <T> async(
        dispatcher: ModuleDispatcher = ModuleDispatcher.Async,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T>

    // Bukkit 메인 스레드에서 실행 (suspend: 현재 코루틴은 대기)
    suspend fun <T> onSync(block: suspend CoroutineScope.() -> T): T

    fun activeJobCount(): Int

    override fun capabilities(): AsyncCapability = AsyncCapability.COROUTINE

    // module unload 시 호출 — scope cancel + cleanup
    override fun close()
}
