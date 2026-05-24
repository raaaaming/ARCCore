package cc.arccore.runtime.lifecycle

import cc.arccore.api.module.ModuleContainer

sealed class LifecycleResult {

    data class Success(val container: ModuleContainer) : LifecycleResult()

    data class Failure(
        val container: ModuleContainer,
        val error: Throwable,
        val rollbackSuccess: Boolean = false
    ) : LifecycleResult()

    /** 지정한 모듈 ID가 레지스트리에 존재하지 않을 때 반환됩니다. */
    data class NotFound(val moduleId: String) : LifecycleResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isNotFound: Boolean get() = this is NotFound
}

interface ModuleLifecycleManager {

    fun enable(container: ModuleContainer): LifecycleResult

    fun disable(container: ModuleContainer): LifecycleResult

    fun unload(container: ModuleContainer): LifecycleResult

    fun enableAll(containers: List<ModuleContainer>): List<LifecycleResult>

    fun disableAll(containers: List<ModuleContainer>): List<LifecycleResult>

    fun unloadAll(containers: List<ModuleContainer>): List<LifecycleResult>

    fun getCleanupRegistry(): ModuleCleanupRegistry
}
