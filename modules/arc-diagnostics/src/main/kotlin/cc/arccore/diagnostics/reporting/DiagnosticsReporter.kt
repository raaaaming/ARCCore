package cc.arccore.diagnostics.reporting

import cc.arccore.api.module.ModuleState
import cc.arccore.diagnostics.model.ModuleSnapshot
import cc.arccore.diagnostics.model.RuntimeMetrics
import cc.arccore.diagnostics.model.RuntimeSnapshot

class DiagnosticsReporter {

    fun formatFull(snapshot: RuntimeSnapshot): List<String> {
        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §fRuntime Diagnostics §7━━━━━━━━━━━━━━━━━━━━━━━━"
        lines += "§7 Modules: §f${snapshot.enabledModules}§7 enabled / §f${snapshot.totalModules}§7 total" +
            (if (snapshot.failedModules > 0) " §c(${snapshot.failedModules} failed)" else "")
        lines += "§7 Services: §f${snapshot.totalActiveServices}§7 | Commands: §f${snapshot.totalActiveCommands}" +
            "§7 | Listeners: §f${snapshot.totalActiveListeners}§7 | Async: §f${snapshot.totalActiveAsyncTasks}"
        lines += "§7 Memory: §f${snapshot.memoryUsedMb}MB§7 / §f${snapshot.memoryMaxMb}MB §7(${snapshot.memoryUsedPercent}%)"
        lines += "§7 Uptime: §f${formatUptime(snapshot.uptimeMs)}"
        lines += "§7─────────────────────────────────────────────────────────"
        for (module in snapshot.modules.sortedWith(compareBy({ it.state.ordinal }, { it.moduleId }))) {
            lines += formatModuleLine(module)
        }
        lines += "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        return lines
    }

    fun formatModuleDetail(snapshot: ModuleSnapshot): List<String> {
        val lines = mutableListOf<String>()
        val stateColor = stateColor(snapshot.state)
        lines += "§7[ARCCore] §fModule: §e${snapshot.moduleId}§7 v${snapshot.version}"
        lines += "§7 State: ${stateColor}${snapshot.state}"
        lines += "§7 Reload gen: §f${snapshot.reloadGeneration}${if (snapshot.reloadGeneration > 0) " §7(reloaded)" else ""}"
        lines += "§7 ClassLoader: §f${snapshot.classLoaderId ?: "unknown"}"
        lines += "§7 Services (${snapshot.activeServices.size}): §f${snapshot.activeServices.joinToString(", ").ifEmpty { "none" }}"
        lines += "§7 Commands (${snapshot.activeCommands.size}): §f${snapshot.activeCommands.joinToString(", ").ifEmpty { "none" }}"
        lines += "§7 Listeners (${snapshot.activeListeners.size}): §f${snapshot.activeListeners.joinToString(", ").ifEmpty { "none" }}"
        lines += "§7 Async tasks: §f${snapshot.activeAsyncTasks}"
        if (snapshot.dependencies.isNotEmpty()) {
            lines += "§7 Dependencies: §f${snapshot.dependencies.joinToString(", ")}"
        }
        if (snapshot.failureCause != null) {
            lines += "§c Failure: ${snapshot.failureCause}"
        }
        return lines
    }

    fun formatMetrics(metrics: RuntimeMetrics): List<String> {
        val lines = mutableListOf<String>()
        lines += "§7[ARCCore] §fRuntime Metrics"
        lines += "§7 Modules: §f${metrics.enabledModules}§7 / §f${metrics.totalModules} §7(failed: §c${metrics.failedModules}§7)"
        lines += "§7 Total Reloads: §f${metrics.totalReloads}"
        lines += "§7 Services: §f${metrics.totalActiveServices} §7| Commands: §f${metrics.totalActiveCommands}"
        lines += "§7 Listeners: §f${metrics.totalActiveListeners} §7| Async: §f${metrics.totalActiveAsyncTasks}"
        lines += "§7 Memory: §f${metrics.memoryUsedMb}MB §7/ §f${metrics.memoryMaxMb}MB §7(${metrics.memoryUsedPercent}%)"
        lines += "§7 Uptime: §f${formatUptime(metrics.uptimeMs)}"
        return lines
    }

    private fun formatModuleLine(snapshot: ModuleSnapshot): String {
        val stateColor = stateColor(snapshot.state)
        val gen = if (snapshot.reloadGeneration > 0) "§7[r${snapshot.reloadGeneration}]" else ""
        return "§7  ${stateColor}●§7 §f${snapshot.moduleId}§7 $gen" +
            " svc:§f${snapshot.activeServices.size}§7 cmd:§f${snapshot.activeCommands.size}" +
            "§7 lst:§f${snapshot.activeListeners.size}§7 async:§f${snapshot.activeAsyncTasks}"
    }

    private fun stateColor(state: ModuleState): String = when (state) {
        ModuleState.ENABLED -> "§a"
        ModuleState.DISABLED -> "§e"
        ModuleState.FAILED -> "§c"
        ModuleState.UNLOADED -> "§8"
        ModuleState.CREATED -> "§7"
        ModuleState.LOADED -> "§7"
        ModuleState.ENABLING -> "§b"
        ModuleState.DISABLING -> "§6"
        ModuleState.UNLOADING -> "§8"
    }

    private fun formatUptime(ms: Long): String {
        val seconds = ms / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        return "${hours}h ${minutes % 60}m ${seconds % 60}s"
    }
}
