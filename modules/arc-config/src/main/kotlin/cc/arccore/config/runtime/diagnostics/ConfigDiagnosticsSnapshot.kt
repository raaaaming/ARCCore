package cc.arccore.config.runtime.diagnostics

data class ConfigDiagnosticsSnapshot(
    val moduleId: String,
    val loadedConfigs: Int,
    val activeWatchers: Int,
    val currentGeneration: Long,
    val totalReloads: Long,
    val totalLoads: Long,
    val cachedEntries: Int
)
