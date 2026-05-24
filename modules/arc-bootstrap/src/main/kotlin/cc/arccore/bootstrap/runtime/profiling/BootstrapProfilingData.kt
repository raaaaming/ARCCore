package cc.arccore.bootstrap.runtime.profiling

import cc.arccore.bootstrap.runtime.BootstrapPhase

data class BootstrapProfilingData(
    val moduleId: String,
    val entries: List<PhaseTimingEntry>,
    val totalStartNanos: Long,
    val totalEndNanos: Long
) {
    val totalDurationNanos: Long get() = totalEndNanos - totalStartNanos
    val totalDurationMs: Double get() = totalDurationNanos / 1_000_000.0

    fun entryFor(phase: BootstrapPhase): PhaseTimingEntry? = entries.find { it.phase == phase }

    fun slowestPhase(): PhaseTimingEntry? = entries.maxByOrNull { it.durationNanos }

    fun failedPhases(): List<PhaseTimingEntry> = entries.filter { !it.success }

    fun summary(): String = buildString {
        appendLine("Bootstrap profiling for '$moduleId' (total: ${String.format("%.2f", totalDurationMs)}ms):")
        entries.forEach { entry ->
            val status = if (entry.success) "OK" else "FAIL"
            appendLine("  [${entry.phase}] ${String.format("%.2f", entry.durationMs)}ms [$status]")
        }
    }
}
