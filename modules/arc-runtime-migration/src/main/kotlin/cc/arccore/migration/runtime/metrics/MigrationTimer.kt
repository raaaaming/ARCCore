package cc.arccore.migration.runtime.metrics

import cc.arccore.migration.runtime.model.MigrationPhase
import java.util.concurrent.ConcurrentHashMap

internal class MigrationTimer {
    private val timings = ConcurrentHashMap<MigrationPhase, LongArray>()

    fun start(phase: MigrationPhase) {
        timings[phase] = longArrayOf(System.currentTimeMillis(), 0L)
    }

    fun stop(phase: MigrationPhase): Long {
        val entry = timings[phase] ?: return 0L
        val endMs = System.currentTimeMillis()
        entry[1] = endMs
        return endMs - entry[0]
    }

    fun elapsed(phase: MigrationPhase): Long {
        val entry = timings[phase] ?: return 0L
        val endMs = if (entry[1] > 0L) entry[1] else System.currentTimeMillis()
        return endMs - entry[0]
    }

    fun allTimings(): Map<MigrationPhase, Long> {
        return timings.mapValues { (_, entry) ->
            val endMs = if (entry[1] > 0L) entry[1] else System.currentTimeMillis()
            endMs - entry[0]
        }
    }
}
