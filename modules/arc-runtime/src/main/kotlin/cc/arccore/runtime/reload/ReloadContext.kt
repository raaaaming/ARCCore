package cc.arccore.runtime.reload

import java.nio.file.Path

internal class ReloadContext(
    val targetModuleId: String,
    val jarPath: Path,
    val affectedModuleIds: List<String>,
    val isStateful: Boolean,
    val startTimeMs: Long = System.currentTimeMillis()
) {
    var phase: ReloadPhase = ReloadPhase.IDLE
    val capturedStates: MutableMap<String, Map<String, Any?>> = mutableMapOf()
    val disabledModules: MutableList<String> = mutableListOf()
    val enabledModules: MutableList<String> = mutableListOf()
    val elapsedMs: Long get() = System.currentTimeMillis() - startTimeMs
}
