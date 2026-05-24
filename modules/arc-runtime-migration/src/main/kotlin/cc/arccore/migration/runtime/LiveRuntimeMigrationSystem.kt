package cc.arccore.migration.runtime

import cc.arccore.migration.runtime.lifecycle.MigrationLifecycleObserver
import cc.arccore.migration.runtime.model.MigrationId
import cc.arccore.migration.runtime.model.MigrationMetrics
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.model.MigrationResult
import cc.arccore.migration.runtime.model.NodeDescriptor
import cc.arccore.migration.runtime.state.MigrationSession

interface LiveRuntimeMigrationSystem {
    fun migrate(sourceNodeId: String, targetNodeId: String, moduleId: String): MigrationResult
    fun migrateAll(sourceNodeId: String, targetNodeId: String, moduleIds: List<String>): Map<String, MigrationResult>
    fun getSession(migrationId: MigrationId): MigrationSession?
    fun getActiveSessions(): List<MigrationSession>
    fun isModuleMigrating(moduleId: String): Boolean
    fun registerNode(node: NodeDescriptor)
    fun unregisterNode(nodeId: String)
    fun getRegisteredNodes(): List<NodeDescriptor>
    fun getCurrentPhase(migrationId: MigrationId): MigrationPhase?
    fun abortMigration(migrationId: MigrationId): Boolean
    fun getMetrics(): MigrationMetrics
    fun addObserver(observer: MigrationLifecycleObserver)
    fun removeObserver(observer: MigrationLifecycleObserver)
    fun supportsRollingMigration(): Boolean = false
    fun supportsMultiNodeConsensus(): Boolean = false
}
