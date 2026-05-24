package cc.arccore.migration.runtime.integration

import cc.arccore.migration.runtime.LiveRuntimeMigrationSystem
import cc.arccore.migration.runtime.model.MigrationResult

class ZeroDowntimeMigrationBridge(
    private val migrationSystem: LiveRuntimeMigrationSystem
) {
    fun shouldMigrateInsteadOfReload(moduleId: String): Boolean = false

    fun migrateAsReload(moduleId: String, sourceNodeId: String, targetNodeId: String): MigrationResult {
        return migrationSystem.migrate(sourceNodeId, targetNodeId, moduleId)
    }
}
