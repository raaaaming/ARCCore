package cc.arccore.diagnostics.metrics

import cc.arccore.diagnostics.model.RuntimeMetrics
import cc.arccore.diagnostics.model.RuntimeSnapshot
import cc.arccore.diagnostics.tracking.DiagnosticsRegistry

class RuntimeMetricsCollector(private val registry: DiagnosticsRegistry) {

    fun collect(snapshot: RuntimeSnapshot): RuntimeMetrics {
        val totalReloads = registry.allEntries().sumOf { it.reloadGeneration }
        return RuntimeMetrics(
            totalModules = snapshot.totalModules,
            enabledModules = snapshot.enabledModules,
            failedModules = snapshot.failedModules,
            totalReloads = totalReloads,
            totalActiveServices = snapshot.totalActiveServices,
            totalActiveCommands = snapshot.totalActiveCommands,
            totalActiveListeners = snapshot.totalActiveListeners,
            totalActiveAsyncTasks = snapshot.totalActiveAsyncTasks,
            uptimeMs = snapshot.uptimeMs,
            memoryUsedMb = snapshot.memoryUsedMb,
            memoryMaxMb = snapshot.memoryMaxMb,
            memoryUsedPercent = snapshot.memoryUsedPercent
        )
    }
}
