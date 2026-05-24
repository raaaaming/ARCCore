package cc.arccore.eventbus.runtime.event

/**
 * Priority ordering within a single [PropagationPhase].
 *
 * Listeners with lower [order] values execute first within the same phase.
 *
 * Order: LOW(-100) → NORMAL(0) → HIGH(100) → MONITOR(Int.MAX_VALUE)
 *
 * MONITOR is a special priority reserved for read-only observation.
 * Listeners registered at MONITOR priority must not modify event state.
 */
enum class EventPriority(val order: Int) {
    LOW(-100),
    NORMAL(0),
    HIGH(100),
    MONITOR(Int.MAX_VALUE);

    companion object {
        /**
         * Returns the priority whose [order] is closest to the given [order] value.
         */
        fun fromOrder(order: Int): EventPriority =
            entries.minByOrNull { Math.abs(it.order - order) } ?: NORMAL
    }
}
