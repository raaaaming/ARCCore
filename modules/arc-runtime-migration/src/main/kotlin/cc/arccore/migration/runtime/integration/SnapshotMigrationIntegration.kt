package cc.arccore.migration.runtime.integration

import cc.arccore.migration.runtime.model.MigrationContext

class SnapshotMigrationIntegration {
    private var snapshotProvider: ((String) -> Any?)? = null
    private var recoveryProvider: ((Any) -> Any?)? = null

    fun configure(
        onCaptureSnapshot: (moduleId: String) -> Any?,
        onRecover: (snapshot: Any) -> Any?
    ) {
        snapshotProvider = onCaptureSnapshot
        recoveryProvider = onRecover
    }

    internal fun captureForMigration(context: MigrationContext): Any? {
        return snapshotProvider?.invoke(context.moduleId)
    }

    internal fun restoreOnTarget(context: MigrationContext): Any? {
        val snapshot = context.capturedSnapshot ?: return null
        return recoveryProvider?.invoke(snapshot)
    }

    internal fun isSnapshotValid(context: MigrationContext): Boolean {
        return context.capturedSnapshot != null
    }

    internal fun cleanupAfterMigration(context: MigrationContext): Boolean {
        context.capturedSnapshot?.let { }
        return true
    }
}
