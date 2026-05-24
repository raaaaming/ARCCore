package cc.arccore.diagnostics.model

import java.time.Instant

data class RuntimeSnapshot(
    val timestamp: Instant,
    val uptimeMs: Long,
    val totalModules: Int,
    val enabledModules: Int,
    val disabledModules: Int,
    val failedModules: Int,
    val modules: List<ModuleSnapshot>,
    val totalActiveServices: Int,
    val totalActiveCommands: Int,
    val totalActiveListeners: Int,
    val totalActiveAsyncTasks: Int,
    val memoryUsedMb: Long,
    val memoryMaxMb: Long,
    val memoryUsedPercent: Int
)
