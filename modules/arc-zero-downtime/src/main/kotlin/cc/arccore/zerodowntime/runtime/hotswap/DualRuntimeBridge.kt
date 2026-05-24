package cc.arccore.zerodowntime.runtime.hotswap

import java.time.Instant

internal class DualRuntimeBridge(
    val moduleId: String,
    val oldGeneration: Int,
    val newGeneration: Int,
    private val bridgeCreatedAt: Instant = Instant.now()
) {
    sealed class StateTransferResult {
        data object Success : StateTransferResult()
        data class PartialSuccess(val failedKeys: List<String>) : StateTransferResult()
        data class Failure(val error: Throwable) : StateTransferResult()
    }

    fun transferState(capturedState: Map<String, Any?>, newModuleAccess: StateReceiver): StateTransferResult {
        if (capturedState.isEmpty()) return StateTransferResult.Success

        return try {
            val failedKeys = mutableListOf<String>()
            capturedState.forEach { (key, value) ->
                try {
                    newModuleAccess.receiveState(key, value)
                } catch (e: Exception) {
                    failedKeys.add(key)
                }
            }
            if (failedKeys.isEmpty()) StateTransferResult.Success
            else StateTransferResult.PartialSuccess(failedKeys)
        } catch (e: Exception) {
            StateTransferResult.Failure(e)
        }
    }

    fun dualLiveDurationMs(): Long =
        System.currentTimeMillis() - bridgeCreatedAt.toEpochMilli()

    fun interface StateReceiver {
        fun receiveState(key: String, value: Any?)
    }
}
