package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase

sealed class RecoveryStageResult {
    data object Success : RecoveryStageResult()
    data class Failure(val error: Throwable, val fatalToRecovery: Boolean = true) : RecoveryStageResult()
    data class Skipped(val reason: String) : RecoveryStageResult()
}

internal interface RecoveryPipelineStage {
    val phase: RecoveryPhase
    val rollbackOnFailure: Boolean get() = true
    fun execute(context: RecoveryContext): RecoveryStageResult
}
