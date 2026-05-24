package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.integration.SnapshotMigrationIntegration
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

internal class RestoreStateStage(
    private val snapshotIntegration: SnapshotMigrationIntegration
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.RESTORE_STATE

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val result = snapshotIntegration.restoreOnTarget(context)
            context.restoreResult = result
            MigrationStageResult.Success
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
