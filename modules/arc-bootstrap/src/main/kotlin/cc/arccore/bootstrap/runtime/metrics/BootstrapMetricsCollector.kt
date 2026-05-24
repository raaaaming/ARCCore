package cc.arccore.bootstrap.runtime.metrics

import cc.arccore.bootstrap.runtime.BootstrapPhase
import cc.arccore.bootstrap.runtime.state.BootstrapResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BootstrapMetricsCollector {

    private val moduleMetrics: ConcurrentHashMap<String, ModuleBootstrapMetrics> = ConcurrentHashMap()
    private val attempted = AtomicInteger(0)
    private val succeeded = AtomicInteger(0)
    private val failed = AtomicInteger(0)
    private val skipped = AtomicInteger(0)
    private val collectionStartNanos = System.nanoTime()

    fun recordResult(result: BootstrapResult) {
        attempted.incrementAndGet()

        val metrics = when (result) {
            is BootstrapResult.Success -> {
                succeeded.incrementAndGet()
                ModuleBootstrapMetrics(
                    moduleId = result.moduleId,
                    succeeded = true,
                    skipped = false,
                    failedPhase = null,
                    totalDurationMs = result.profilingData?.totalDurationMs ?: 0.0,
                    phaseTimings = result.profilingData?.entries?.associate { it.phase to it.durationMs } ?: emptyMap(),
                    profilingData = result.profilingData
                )
            }
            is BootstrapResult.Failure -> {
                failed.incrementAndGet()
                ModuleBootstrapMetrics(
                    moduleId = result.moduleId,
                    succeeded = false,
                    skipped = false,
                    failedPhase = result.failedPhase,
                    totalDurationMs = result.profilingData?.totalDurationMs ?: 0.0,
                    phaseTimings = result.profilingData?.entries?.associate { it.phase to it.durationMs } ?: emptyMap(),
                    profilingData = result.profilingData
                )
            }
            is BootstrapResult.Skipped -> {
                skipped.incrementAndGet()
                ModuleBootstrapMetrics(
                    moduleId = result.moduleId,
                    succeeded = false,
                    skipped = true,
                    failedPhase = null,
                    totalDurationMs = 0.0,
                    phaseTimings = emptyMap(),
                    profilingData = null
                )
            }
        }

        moduleMetrics[result.moduleId] = metrics
    }

    fun buildMetrics(): BootstrapMetrics {
        val all = moduleMetrics.values.toList()
        val totalDurationMs = (System.nanoTime() - collectionStartNanos) / 1_000_000.0
        val avgDurationMs = if (all.isEmpty()) 0.0
            else all.sumOf { it.totalDurationMs } / all.size

        val slowest = all.maxByOrNull { it.totalDurationMs }

        val phaseFailureCounts = mutableMapOf<BootstrapPhase, Int>()
        all.filter { it.failed }.forEach { m ->
            m.failedPhase?.let { phase ->
                phaseFailureCounts[phase] = (phaseFailureCounts[phase] ?: 0) + 1
            }
        }

        return BootstrapMetrics(
            totalModulesAttempted = attempted.get(),
            totalModulesSucceeded = succeeded.get(),
            totalModulesFailed = failed.get(),
            totalModulesSkipped = skipped.get(),
            totalBootstrapDurationMs = totalDurationMs,
            averageBootstrapDurationMs = avgDurationMs,
            slowestModuleId = slowest?.moduleId,
            slowestModuleDurationMs = slowest?.totalDurationMs ?: 0.0,
            phaseFailureCounts = phaseFailureCounts
        )
    }

    fun metricsFor(moduleId: String): ModuleBootstrapMetrics? = moduleMetrics[moduleId]

    fun allModuleMetrics(): Map<String, ModuleBootstrapMetrics> = moduleMetrics.toMap()
}
