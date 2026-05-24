package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.draining.DrainConfig
import cc.arccore.zerodowntime.runtime.draining.DrainResult
import cc.arccore.zerodowntime.runtime.draining.InflightCounter
import cc.arccore.zerodowntime.runtime.draining.RequestDrainManager
import cc.arccore.zerodowntime.runtime.draining.SimpleInflightCounter
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

internal class RequestDrainStage(
    private val drainManager: RequestDrainManager = RequestDrainManager(),
    private val counterProvider: (String) -> InflightCounter = { SimpleInflightCounter() }
) : PipelineStage {
    override val phase = ZeroDowntimePhase.REQUEST_DRAIN
    override val rollbackOnFailure = false

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.REQUEST_DRAIN

        val counter = counterProvider(context.targetModuleId)

        return when (drainManager.drain(
            context.targetModuleId,
            counter,
            context.drainRecord
        )) {
            is DrainResult.Completed -> StageResult.Success
            is DrainResult.ForceDrained -> {
                // 강제 드레인은 경고이지만 계속 진행
                StageResult.Success
            }
            is DrainResult.TimedOut -> {
                // 타임아웃도 경고 후 계속 진행 (서비스 연속성 우선)
                StageResult.Success
            }
        }
    }
}
