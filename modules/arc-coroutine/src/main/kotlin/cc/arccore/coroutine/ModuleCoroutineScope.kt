package cc.arccore.coroutine

import kotlinx.coroutines.CoroutineScope

// 모듈 생명주기와 연동된 CoroutineScope wrapper.
// module unload 시 cancel() 이 자동 호출된다.
interface ModuleCoroutineScope {
    val scope: CoroutineScope
    val isActive: Boolean
    fun cancel(reason: String = "Module scope cancelled")
}
