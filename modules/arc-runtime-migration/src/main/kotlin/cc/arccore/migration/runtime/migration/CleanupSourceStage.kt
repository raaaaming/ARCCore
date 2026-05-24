package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.cleanup.MigrationCleanupManager
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

internal class CleanupSourceStage(
    private val cleanupManager: MigrationCleanupManager
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.CLEANUP_SOURCE
    override val rollbackOnFailure: Boolean = false

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            cleanupManager.cleanup(context)
            MigrationStageResult.Success
        } catch (e: Exception) {
            MigrationStageResult.Failure(e, fatalToMigration = false)
        }
    }
}
