package cc.arccore.bootstrap.runtime.optimization

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapContextKey
import cc.arccore.bootstrap.runtime.BootstrapPhase

/**
 * Analyzes BootstrapContext to derive optimization hints before execution.
 * These hints are consumed by BootstrapOrchestrator to prune unnecessary work.
 */
class BootstrapOptimizer {

    fun computeHints(context: BootstrapContext): List<BootstrapOptimizationHint> {
        val hints = mutableListOf<BootstrapOptimizationHint>()

        // Hot-reload: skip DISCOVERY and CLASSLOADER_PREPARE phases if marked for reload-skip
        if (context.isHotReload) {
            BootstrapPhase.entries
                .filter { it.canReloadSkip() }
                .forEach { phase ->
                    hints.add(
                        BootstrapOptimizationHint.SkipPhase(
                            moduleId = context.moduleId,
                            phase = phase,
                            reason = "hot-reload skips phase ${phase.name}"
                        )
                    )
                }
        }

        // If no generated artifacts exist, skip GENERATED_BOOTSTRAP
        val preloaded = context.get(BootstrapContextKey.PRELOADED_METADATA)
        if (preloaded != null && preloaded.artifactManifest.isMissing) {
            hints.add(
                BootstrapOptimizationHint.SkipPhase(
                    moduleId = context.moduleId,
                    phase = BootstrapPhase.GENERATED_BOOTSTRAP,
                    reason = "no generated artifacts found — skipping GENERATED_BOOTSTRAP"
                )
            )
        }

        return hints
    }
}
