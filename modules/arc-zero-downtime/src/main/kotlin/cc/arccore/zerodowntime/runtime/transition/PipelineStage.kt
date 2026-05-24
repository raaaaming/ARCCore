package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

sealed class StageResult {
    data object Success : StageResult()
    data class Failure(val error: Throwable, val fatalToTransition: Boolean = true) : StageResult()
    data class Skipped(val reason: String) : StageResult()
}

internal interface PipelineStage {
    val phase: ZeroDowntimePhase
    val rollbackOnFailure: Boolean get() = true
    fun execute(context: TransitionContext): StageResult
}
