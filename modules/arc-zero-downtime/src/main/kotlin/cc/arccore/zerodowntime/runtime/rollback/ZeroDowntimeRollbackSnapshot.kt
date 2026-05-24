package cc.arccore.zerodowntime.runtime.rollback

import java.time.Instant

internal data class ZeroDowntimeRollbackSnapshot(
    val moduleId: String,
    val oldGeneration: Int,
    val capturedModuleStates: Map<String, Map<String, Any?>> = emptyMap(),
    val dependentModuleIds: List<String> = emptyList(),
    val snapshotAt: Instant = Instant.now()
)
