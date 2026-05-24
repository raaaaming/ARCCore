package cc.arccore.bootstrap.runtime.optimization

import cc.arccore.bootstrap.runtime.BootstrapPhase

/**
 * Optimization hints that the BootstrapOptimizer can derive from module metadata
 * and apply to the bootstrap execution plan.
 */
sealed class BootstrapOptimizationHint {

    /** Skip a phase entirely for this module — safe only if known to be no-op. */
    data class SkipPhase(
        val moduleId: String,
        val phase: BootstrapPhase,
        val reason: String
    ) : BootstrapOptimizationHint()

    /** Re-use a cached artifact from a previous bootstrap run. */
    data class UseCachedArtifact(
        val moduleId: String,
        val artifactKey: String,
        val cachedValue: Any
    ) : BootstrapOptimizationHint()

    /** Elevate module to a higher priority tier in the execution plan. */
    data class PrioritizeModule(
        val moduleId: String,
        val reason: String
    ) : BootstrapOptimizationHint()

    /** Defer a module's bootstrap to the POST_ENABLE phase. */
    data class DeferBootstrap(
        val moduleId: String,
        val reason: String
    ) : BootstrapOptimizationHint()
}
