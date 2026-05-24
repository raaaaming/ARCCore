package cc.arccore.snapshot.runtime.ownership

import cc.arccore.snapshot.runtime.recovery.RecoverableRuntime

class OwnershipRecoveryManager(
    private val recoverables: MutableMap<String, RecoverableRuntime> = mutableMapOf()
) {
    sealed class OwnershipRestoreResult {
        data class Success(val restoredOwnerships: Int) : OwnershipRestoreResult()
        data class PartialSuccess(val restoredOwnerships: Int, val skipped: Int) : OwnershipRestoreResult()
        data class Failure(val error: Throwable) : OwnershipRestoreResult()
    }

    fun register(runtime: RecoverableRuntime) {
        recoverables[runtime.runtimeId] = runtime
    }

    fun restoreOwnership(
        runtimeId: String,
        ownershipState: Map<String, Any?>
    ): OwnershipRestoreResult {
        val runtime = recoverables[runtimeId]
            ?: return OwnershipRestoreResult.PartialSuccess(0, 1)

        if (!runtime.supportsOwnershipRecovery()) {
            return OwnershipRestoreResult.PartialSuccess(0, 1)
        }

        return when (val result = runtime.recoverOwnership(ownershipState)) {
            is RecoverableRuntime.OwnershipRecoveryResult.Success ->
                OwnershipRestoreResult.Success(result.restoredOwnerships)
            is RecoverableRuntime.OwnershipRecoveryResult.Failure ->
                OwnershipRestoreResult.Failure(result.error)
            is RecoverableRuntime.OwnershipRecoveryResult.Unsupported ->
                OwnershipRestoreResult.PartialSuccess(0, 1)
        }
    }
}
