package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.model.MigrationResult

internal interface MigrationLifecycleObserver {
    fun onPhaseStarted(context: MigrationContext, phase: MigrationPhase) {}
    fun onPhaseCompleted(context: MigrationContext, phase: MigrationPhase) {}
    fun onPhaseFailed(context: MigrationContext, phase: MigrationPhase, error: Throwable) {}
    fun onMigrationCompleted(result: MigrationResult) {}
    fun onRollbackStarted(context: MigrationContext, failedPhase: MigrationPhase) {}
    fun onRollbackCompleted(context: MigrationContext, success: Boolean) {}
}
