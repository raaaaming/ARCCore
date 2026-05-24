package cc.arccore.zerodowntime.runtime.state

import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

data class TransitionState(
    val moduleId: String,
    val phase: ZeroDowntimePhase,
    val oldGeneration: Int,
    val newGeneration: Int?,
    val elapsedMs: Long,
    val inflightCount: Int = 0,
    val canAbort: Boolean = true
)
