package cc.arccore.bootstrap.runtime.profiling

import cc.arccore.bootstrap.runtime.BootstrapPhase

data class PhaseTimingEntry(
    val phase: BootstrapPhase,
    val startNanos: Long,
    val endNanos: Long,
    val success: Boolean,
    val notes: List<String> = emptyList()
) {
    val durationNanos: Long get() = endNanos - startNanos
    val durationMs: Double get() = durationNanos / 1_000_000.0

    override fun toString(): String =
        "PhaseTimingEntry(phase=$phase, duration=${String.format("%.2f", durationMs)}ms, success=$success)"
}
