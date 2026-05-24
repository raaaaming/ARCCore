package cc.arccore.zerodowntime.runtime.validation

import cc.arccore.zerodowntime.runtime.hotswap.ReadinessResult
import cc.arccore.zerodowntime.runtime.hotswap.ZeroDowntimeReadinessProbe

internal class NewRuntimeValidator {
    data class ValidationReport(
        val compatible: Boolean,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
    )

    sealed class ReadinessCheckResult {
        data object Ready : ReadinessCheckResult()
        data class NotReady(val reason: String) : ReadinessCheckResult()
        data class Failed(val cause: Throwable) : ReadinessCheckResult()
    }

    fun validateBasic(newModuleId: String): ValidationReport {
        return ValidationReport(compatible = true)
    }

    fun checkReadinessProbe(probe: ZeroDowntimeReadinessProbe, timeoutMs: Long): ReadinessCheckResult {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            return when (val result = probe.isReady()) {
                is ReadinessResult.Ready -> ReadinessCheckResult.Ready
                is ReadinessResult.NotReady -> {
                    Thread.sleep(minOf(result.retryAfterMs, deadline - System.currentTimeMillis()))
                    continue
                }
                is ReadinessResult.Failed -> ReadinessCheckResult.Failed(result.cause)
            }
        }
        return ReadinessCheckResult.NotReady("Readiness probe timed out after ${timeoutMs}ms")
    }
}
