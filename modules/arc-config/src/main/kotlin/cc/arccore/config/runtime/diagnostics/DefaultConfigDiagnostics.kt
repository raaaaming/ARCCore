package cc.arccore.config.runtime.diagnostics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Collects per-module metrics for the config runtime.
 *
 * All counters are [AtomicLong] for thread safety. Metrics are accumulated
 * globally and can be queried per module via [snapshot].
 */
class DefaultConfigDiagnostics {

    private val totalLoads = AtomicLong(0L)
    private val totalReloads = AtomicLong(0L)

    // Per-module load counts
    private val moduleLoads: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap()

    // Per-module reload counts
    private val moduleReloads: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap()

    fun recordLoad(moduleId: String) {
        totalLoads.incrementAndGet()
        moduleLoads.computeIfAbsent(moduleId) { AtomicLong(0L) }.incrementAndGet()
    }

    fun recordReload(moduleId: String) {
        totalReloads.incrementAndGet()
        moduleReloads.computeIfAbsent(moduleId) { AtomicLong(0L) }.incrementAndGet()
    }

    fun snapshot(
        moduleId: String,
        loadedConfigs: Int,
        activeWatchers: Int,
        currentGeneration: Long,
        cachedEntries: Int
    ): ConfigDiagnosticsSnapshot = ConfigDiagnosticsSnapshot(
        moduleId = moduleId,
        loadedConfigs = loadedConfigs,
        activeWatchers = activeWatchers,
        currentGeneration = currentGeneration,
        totalReloads = moduleReloads[moduleId]?.get() ?: 0L,
        totalLoads = moduleLoads[moduleId]?.get() ?: 0L,
        cachedEntries = cachedEntries
    )

    fun globalTotalLoads(): Long = totalLoads.get()
    fun globalTotalReloads(): Long = totalReloads.get()
}
