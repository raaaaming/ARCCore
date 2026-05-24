package cc.arccore.bootstrap.runtime.metrics

import cc.arccore.bootstrap.runtime.BootstrapPhase

data class BootstrapMetrics(
    val totalModulesAttempted: Int,
    val totalModulesSucceeded: Int,
    val totalModulesFailed: Int,
    val totalModulesSkipped: Int,
    val totalBootstrapDurationMs: Double,
    val averageBootstrapDurationMs: Double,
    val slowestModuleId: String?,
    val slowestModuleDurationMs: Double,
    val phaseFailureCounts: Map<BootstrapPhase, Int>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val successRate: Double
        get() = if (totalModulesAttempted == 0) 0.0
                else totalModulesSucceeded.toDouble() / totalModulesAttempted.toDouble()
}
