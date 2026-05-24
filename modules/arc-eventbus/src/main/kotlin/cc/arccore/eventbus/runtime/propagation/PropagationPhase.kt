package cc.arccore.eventbus.runtime.propagation

/**
 * Execution phases for event propagation.
 *
 * Phases execute in [order] order: PRE_PROCESS → NORMAL → POST_PROCESS → MONITOR.
 *
 * - **PRE_PROCESS(-200):** Pre-processing, validation, or enrichment before main handling.
 * - **NORMAL(0):** Standard business logic handling.
 * - **POST_PROCESS(200):** Post-processing, side-effects, or audit after main handling.
 * - **MONITOR(Int.MAX_VALUE):** Read-only observation. Always executes, even for cancelled events.
 *   Listeners in this phase must not modify event state.
 */
enum class PropagationPhase(val order: Int) {
    PRE_PROCESS(-200),
    NORMAL(0),
    POST_PROCESS(200),
    MONITOR(Int.MAX_VALUE);

    companion object {
        /**
         * Returns phases sorted by execution order (ascending).
         */
        val sortedByOrder: List<PropagationPhase> by lazy {
            entries.sortedBy { it.order }
        }
    }
}
