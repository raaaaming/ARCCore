package cc.arccore.bootstrap.runtime.metrics

import cc.arccore.bootstrap.runtime.BootstrapPhase
import cc.arccore.bootstrap.runtime.profiling.BootstrapProfilingData

data class ModuleBootstrapMetrics(
    val moduleId: String,
    val succeeded: Boolean,
    val skipped: Boolean,
    val failedPhase: BootstrapPhase?,
    val totalDurationMs: Double,
    val phaseTimings: Map<BootstrapPhase, Double>,
    val profilingData: BootstrapProfilingData?
) {
    val failed: Boolean get() = !succeeded && !skipped

    fun slowestPhase(): Pair<BootstrapPhase, Double>? =
        phaseTimings.maxByOrNull { it.value }?.toPair()
}
