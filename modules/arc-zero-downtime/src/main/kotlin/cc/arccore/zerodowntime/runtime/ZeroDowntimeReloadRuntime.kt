package cc.arccore.zerodowntime.runtime

import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleObserver
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeMetrics
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeReloadResult
import cc.arccore.zerodowntime.runtime.state.TransitionState

interface ZeroDowntimeReloadRuntime {
    /**
     * 단일 모듈을 zero-downtime 방식으로 reload한다.
     * OLD runtime이 살아있는 상태에서 NEW runtime을 부트스트랩하고,
     * 소유권 이전 및 graceful drain 후 OLD를 정리한다.
     */
    fun reload(moduleId: String): ZeroDowntimeReloadResult

    /**
     * 여러 모듈을 순차적으로 zero-downtime reload한다.
     */
    fun reloadAll(moduleIds: List<String>): Map<String, ZeroDowntimeReloadResult>

    fun isTransitioning(moduleId: String): Boolean

    fun getTransitionState(moduleId: String): TransitionState?

    fun getActiveTransitions(): List<TransitionState>

    fun getMetrics(): ZeroDowntimeMetrics

    fun addObserver(observer: ZeroDowntimeLifecycleObserver)
    fun removeObserver(observer: ZeroDowntimeLifecycleObserver)
}
