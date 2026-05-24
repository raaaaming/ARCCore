package cc.arccore.diagnostics.model

data class RuntimeMetrics(
    val totalModules: Int,
    val enabledModules: Int,
    val failedModules: Int,
    val totalReloads: Int,          // sum of reloadGeneration across all modules
    val totalActiveServices: Int,
    val totalActiveCommands: Int,
    val totalActiveListeners: Int,
    val totalActiveAsyncTasks: Int,
    val uptimeMs: Long,
    val memoryUsedMb: Long,
    val memoryMaxMb: Long,
    val memoryUsedPercent: Int
)
