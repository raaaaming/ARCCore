package cc.arccore.zerodowntime.runtime.coordination

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class GenerationCounter {
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    fun current(moduleId: String): Int = counters[moduleId]?.get() ?: 0

    fun increment(moduleId: String): Int {
        return counters.computeIfAbsent(moduleId) { AtomicInteger(0) }.incrementAndGet()
    }

    fun isStale(moduleId: String, generation: Int): Boolean {
        return current(moduleId) != generation
    }

    fun initialize(moduleId: String, generation: Int) {
        counters.computeIfAbsent(moduleId) { AtomicInteger(generation) }
    }
}
