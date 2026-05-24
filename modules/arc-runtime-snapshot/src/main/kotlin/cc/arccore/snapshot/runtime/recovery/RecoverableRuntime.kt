package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot

interface RecoverableRuntime {
    val runtimeId: String

    fun recover(snapshot: RuntimeSnapshot): RecoveryApplyResult

    fun supportsOwnershipRecovery(): Boolean = false

    fun recoverOwnership(ownershipState: Map<String, Any?>): OwnershipRecoveryResult =
        OwnershipRecoveryResult.Unsupported

    fun canRecoverFrom(snapshot: RuntimeSnapshot): Boolean = true

    // 미래 확장 포인트
    fun supportsEventSourcedRecovery(): Boolean = false

    sealed class RecoveryApplyResult {
        data class Success(val restoredEntries: Int) : RecoveryApplyResult()
        data class PartialSuccess(val restoredEntries: Int, val warnings: List<String>) : RecoveryApplyResult()
        data class Failure(val error: Throwable) : RecoveryApplyResult()
    }

    sealed class OwnershipRecoveryResult {
        data class Success(val restoredOwnerships: Int) : OwnershipRecoveryResult()
        data class Failure(val error: Throwable) : OwnershipRecoveryResult()
        data object Unsupported : OwnershipRecoveryResult()
    }
}
