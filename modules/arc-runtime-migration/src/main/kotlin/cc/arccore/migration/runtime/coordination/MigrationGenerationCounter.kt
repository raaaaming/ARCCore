package cc.arccore.migration.runtime.coordination

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class MigrationGenerationCounter {
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    fun current(moduleId: String): Int = counters.getOrDefault(moduleId, AtomicInteger(0)).get()

    fun increment(moduleId: String): Int = counters.computeIfAbsent(moduleId) { AtomicInteger(0) }.incrementAndGet()

    fun isStale(moduleId: String, generation: Int): Boolean = current(moduleId) != generation
}
