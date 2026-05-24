package cc.arccore.bootstrap.runtime.scheduling

import cc.arccore.bootstrap.runtime.BootstrapContext

/**
 * An ordered execution plan produced by [BootstrapOrchestrator].
 *
 * [parallelTiers] is a list of "tiers", where all modules within a tier
 * have no dependency on each other and could theoretically run in parallel.
 * Currently, each tier is executed sequentially — the structure is in place
 * for future parallel bootstrap support.
 */
data class BootstrapExecutionPlan(
    val parallelTiers: List<List<BootstrapContext>>,
    val totalModuleCount: Int,
    val dependencyOrder: List<String>
) {
    /** Flattened sequential order of all contexts. */
    fun sequentialOrder(): List<BootstrapContext> = parallelTiers.flatten()

    fun tierCount(): Int = parallelTiers.size

    override fun toString(): String =
        "BootstrapExecutionPlan(tiers=${parallelTiers.size}, modules=$totalModuleCount, order=$dependencyOrder)"
}
