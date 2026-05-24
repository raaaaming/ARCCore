package cc.arccore.migration.runtime.state

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationId
import cc.arccore.migration.runtime.model.MigrationPhase
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

internal class MigrationSessionRegistry {
    private val activeByMigrationId = ConcurrentHashMap<String, MigrationContext>()
    private val activeByModuleId = ConcurrentHashMap<String, String>()

    fun begin(context: MigrationContext): Boolean {
        val prev = activeByModuleId.putIfAbsent(context.moduleId, context.migrationId.value)
        if (prev != null) return false
        activeByMigrationId[context.migrationId.value] = context
        return true
    }

    fun complete(migrationId: MigrationId) {
        val ctx = activeByMigrationId.remove(migrationId.value) ?: return
        activeByModuleId.remove(ctx.moduleId)
        ctx.phase = MigrationPhase.COMPLETED
    }

    fun fail(migrationId: MigrationId) {
        val ctx = activeByMigrationId.remove(migrationId.value) ?: return
        activeByModuleId.remove(ctx.moduleId)
        ctx.phase = MigrationPhase.FAILED
    }

    fun abort(migrationId: MigrationId): Boolean {
        val ctx = activeByMigrationId.remove(migrationId.value) ?: return false
        activeByModuleId.remove(ctx.moduleId)
        ctx.phase = MigrationPhase.ABORTED
        return true
    }

    fun isModuleMigrating(moduleId: String): Boolean = activeByModuleId.containsKey(moduleId)

    fun getContext(migrationId: MigrationId): MigrationContext? = activeByMigrationId[migrationId.value]

    fun getContextByModule(moduleId: String): MigrationContext? {
        val migrationIdValue = activeByModuleId[moduleId] ?: return null
        return activeByMigrationId[migrationIdValue]
    }

    fun getAllActive(): List<MigrationContext> = activeByMigrationId.values.toList()

    fun toSession(context: MigrationContext): MigrationSession = MigrationSession(
        migrationId = context.migrationId,
        moduleId = context.moduleId,
        sourceNodeId = context.sourceNodeId,
        targetNodeId = context.targetNodeId,
        phase = context.phase,
        startedAt = Instant.ofEpochMilli(context.startTimeMs),
        elapsedMs = context.elapsedMs,
        canAbort = !context.phase.isTerminal() && context.rollbackAvailable,
        snapshotCaptured = context.capturedSnapshot != null
    )
}
