package cc.arccore.migration.runtime.cleanup

import cc.arccore.migration.runtime.draining.MigrationDrainCoordinator
import cc.arccore.migration.runtime.model.MigrationContext

internal class MigrationCleanupManager(
    private val snapshotCleaner: MigrationSnapshotCleaner,
    private val drainCoordinator: MigrationDrainCoordinator
) {
    fun cleanup(context: MigrationContext): CleanupResult {
        val snapshotDeleted = try {
            snapshotCleaner.cleanupAll(context.migrationId) > 0
        } catch (_: Exception) {
            false
        }
        val gateReleased = try {
            drainCoordinator.forceReleaseDrain(context.moduleId)
            true
        } catch (_: Exception) {
            false
        }
        return CleanupResult(snapshotDeleted, gateReleased, true, emptyList())
    }

    fun cleanupOnAbort(context: MigrationContext): CleanupResult = cleanup(context)

    fun cleanupOnRollback(context: MigrationContext): CleanupResult = cleanup(context)

    data class CleanupResult(
        val snapshotDeleted: Boolean,
        val gateReleased: Boolean,
        val sessionRemoved: Boolean,
        val errors: List<Throwable>
    )
}
