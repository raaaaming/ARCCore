package cc.arccore.migration.runtime.model

sealed class MigrationResult {
    data class Success(
        val migrationId: MigrationId,
        val moduleId: String,
        val sourceNodeId: String,
        val targetNodeId: String,
        val totalDurationMs: Long,
        val drainDurationMs: Long,
        val snapshotSizeBytes: Long,
        val transferStats: MigrationTransferStats
    ) : MigrationResult()

    data class Failure(
        val migrationId: MigrationId,
        val moduleId: String,
        val phase: MigrationPhase,
        val error: Throwable,
        val rollbackSuccess: Boolean
    ) : MigrationResult()

    data class Aborted(
        val migrationId: MigrationId,
        val moduleId: String,
        val phase: MigrationPhase,
        val reason: String
    ) : MigrationResult()
}
