package cc.arccore.diagnostics.leak.model

data class LeakReport(
    val moduleId: String,
    val type: LeakType,
    val severity: LeakSeverity,
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap()
)
