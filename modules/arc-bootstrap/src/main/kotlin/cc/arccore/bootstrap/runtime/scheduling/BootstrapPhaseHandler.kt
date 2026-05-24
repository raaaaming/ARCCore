package cc.arccore.bootstrap.runtime.scheduling

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapPhase
import cc.arccore.bootstrap.runtime.state.BootstrapPhaseResult

/**
 * Handles a single bootstrap phase for a given BootstrapContext.
 * Each implementation is responsible for exactly one phase.
 */
interface BootstrapPhaseHandler {

    val phase: BootstrapPhase

    fun handle(context: BootstrapContext): BootstrapPhaseResult

    /**
     * Whether this handler can be safely skipped when [hints] indicate so.
     * Default: true (all phases are skip-eligible if an optimization hint requests it).
     */
    fun isSkippable(): Boolean = true
}
