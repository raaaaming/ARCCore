package cc.arccore.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

// SupervisorJob 기반: 자식 코루틴 하나가 실패해도 다른 자식에 영향 없음.
// CoroutineExceptionHandler: 처리되지 않은 예외를 로깅.
class DefaultModuleCoroutineScope(
    baseContext: CoroutineContext,
    private val exceptionHandler: CoroutineExceptionHandler
) : ModuleCoroutineScope {

    private val supervisorJob: Job = SupervisorJob()

    override val scope: CoroutineScope =
        CoroutineScope(baseContext + supervisorJob + exceptionHandler)

    override val isActive: Boolean get() = supervisorJob.isActive

    override fun cancel(reason: String) {
        scope.cancel(reason)
    }
}
