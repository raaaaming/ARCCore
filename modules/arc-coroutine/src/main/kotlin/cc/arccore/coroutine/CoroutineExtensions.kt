package cc.arccore.coroutine

import cc.arccore.runtime.context.RuntimeModuleContext

// arc-runtime 모듈에 직접 의존 없이 사용 가능한 확장 함수.
// 예: context.coroutineRuntime()?.launch { ... }
fun RuntimeModuleContext.coroutineRuntime(): CoroutineRuntime? =
    asyncRuntime as? CoroutineRuntime
