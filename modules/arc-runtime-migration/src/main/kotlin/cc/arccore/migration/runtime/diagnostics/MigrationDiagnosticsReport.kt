package cc.arccore.migration.runtime.diagnostics

import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.model.MigrationTransferStats
import java.time.Instant

data class MigrationDiagnosticsReport(
    val moduleId: String,
    val migrationHistory: List<MigrationRecord>,
    val averageMigrationMs: Double,
    val averageDrainMs: Double,
    val successRate: Double,
    val lastTransferStats: MigrationTransferStats?
)

data class MigrationRecord(
    val migrationId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val phase: MigrationPhase,
    val success: Boolean,
    val durationMs: Long,
    val rollbackOccurred: Boolean,
    val timestamp: Instant
)
