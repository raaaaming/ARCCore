package cc.arccore.bootstrap.runtime.scheduling

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapPipeline
import cc.arccore.bootstrap.runtime.state.BootstrapResult
import cc.arccore.bootstrap.runtime.metrics.BootstrapMetrics
import cc.arccore.bootstrap.runtime.metrics.BootstrapMetricsCollector

/**
 * Schedules bootstrap execution across multiple modules using [BootstrapOrchestrator]
 * for dependency ordering and [BootstrapPipeline] for per-module phase execution.
 *
 * Currently executes sequentially within each tier.
 * Parallel tier execution is reserved for a future parallel bootstrap feature.
 */
class BootstrapScheduler(
    private val orchestrator: BootstrapOrchestrator,
    private val pipeline: BootstrapPipeline,
    private val metricsCollector: BootstrapMetricsCollector = BootstrapMetricsCollector()
) {

    /**
     * Schedules and executes bootstrap for all given contexts.
     * Returns results in dependency order.
     *
     * @throws cc.arccore.bootstrap.runtime.exception.StartupOptimizationException if cycle detected
     */
    fun scheduleAll(contexts: List<BootstrapContext>): List<BootstrapResult> {
        if (contexts.isEmpty()) return emptyList()

        val plan = orchestrator.sortForBootstrap(contexts)
        val results = mutableListOf<BootstrapResult>()

        for (tier in plan.parallelTiers) {
            // Currently sequential within tiers — parallel support TBD
            for (context in tier) {
                // Populate DEPENDENCY_ORDER slot so DEPENDENCY_GRAPH_BUILD post-condition is satisfied.
                // The orchestrator already computed the full topological order; each module context
                // receives it here before the pipeline runs the DEPENDENCY_GRAPH_BUILD phase.
                context.put(cc.arccore.bootstrap.runtime.BootstrapContextKey.DEPENDENCY_ORDER, plan.dependencyOrder)
                val result = pipeline.execute(context)
                results.add(result)
                metricsCollector.recordResult(result)
            }
        }

        return results
    }

    /**
     * Schedules and executes bootstrap for a single context.
     * Populates DEPENDENCY_ORDER with a singleton list containing the module's own id.
     */
    fun scheduleSingle(context: BootstrapContext): BootstrapResult {
        context.put(cc.arccore.bootstrap.runtime.BootstrapContextKey.DEPENDENCY_ORDER, listOf(context.moduleId))
        val result = pipeline.execute(context)
        metricsCollector.recordResult(result)
        return result
    }

    fun collectMetrics(): BootstrapMetrics = metricsCollector.buildMetrics()
}
