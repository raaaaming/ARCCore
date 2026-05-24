package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.integration.SnapshotMigrationIntegration
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

internal class SnapshotStateStage(
    private val snapshotIntegration: SnapshotMigrationIntegration
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.SNAPSHOT_STATE

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val snapshot = snapshotIntegration.captureForMigration(context)
            if (snapshot != null) {
                context.capturedSnapshot = snapshot
                MigrationStageResult.Success
            } else {
                MigrationStageResult.Failure(IllegalStateException("Snapshot capture returned null for module '${context.moduleId}'"))
            }
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
