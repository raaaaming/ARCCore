package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.coordination.ShadowModuleRegistry
import cc.arccore.zerodowntime.runtime.model.RuntimeHandle
import cc.arccore.zerodowntime.runtime.model.RuntimeRole
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

internal class BootstrapNewStage(
    private val shadowRegistry: ShadowModuleRegistry,
    private val moduleBootstrapper: ModuleBootstrapper
) : PipelineStage {
    override val phase = ZeroDowntimePhase.BOOTSTRAP_NEW
    override val rollbackOnFailure = true

    fun interface ModuleBootstrapper {
        fun bootstrap(moduleId: String, oldGeneration: Int): BootstrapResult
    }

    sealed class BootstrapResult {
        data class Success(val newModuleHandle: Any, val newGeneration: Int) : BootstrapResult()
        data class Failure(val error: Throwable) : BootstrapResult()
    }

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.BOOTSTRAP_NEW

        return when (val result = moduleBootstrapper.bootstrap(
            context.targetModuleId,
            context.oldHandle.generation
        )) {
            is BootstrapResult.Success -> {
                shadowRegistry.register(context.targetModuleId, result.newModuleHandle)
                context.newHandle = RuntimeHandle(
                    moduleId = context.targetModuleId,
                    generation = result.newGeneration,
                    role = RuntimeRole.NEW
                )
                StageResult.Success
            }
            is BootstrapResult.Failure -> {
                StageResult.Failure(result.error)
            }
        }
    }
}
