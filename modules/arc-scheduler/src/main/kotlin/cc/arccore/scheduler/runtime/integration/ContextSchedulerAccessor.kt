package cc.arccore.scheduler.runtime.integration

import cc.arccore.scheduler.runtime.SchedulerRuntime
import java.util.concurrent.ConcurrentHashMap

// RuntimeModuleContext에서 SchedulerRuntime 접근 확장
// 실제 구현 시 ModuleContext 혹은 RuntimeModuleContext에서 제공
// 현재는 SchedulerRuntime을 module attribute로 저장/조회하는 헬퍼

private val schedulerRuntimeMap = ConcurrentHashMap<String, SchedulerRuntime>()

object ContextSchedulerAccessor {
    fun register(moduleId: String, runtime: SchedulerRuntime) {
        schedulerRuntimeMap[moduleId] = runtime
    }

    fun get(moduleId: String): SchedulerRuntime? = schedulerRuntimeMap[moduleId]

    fun remove(moduleId: String) {
        schedulerRuntimeMap.remove(moduleId)
    }
}
