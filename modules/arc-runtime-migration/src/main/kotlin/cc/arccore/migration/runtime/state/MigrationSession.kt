package cc.arccore.migration.runtime.state

import cc.arccore.migration.runtime.model.MigrationId
import cc.arccore.migration.runtime.model.MigrationPhase
import java.time.Instant

data class MigrationSession(
    val migrationId: MigrationId,
    val moduleId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val phase: MigrationPhase,
    val startedAt: Instant,
    val elapsedMs: Long,
    val canAbort: Boolean,
    val snapshotCaptured: Boolean
)
