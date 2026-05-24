package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

sealed class MigrationStageResult {
    data object Success : MigrationStageResult()
    data class Failure(val error: Throwable, val fatalToMigration: Boolean = true) : MigrationStageResult()
    data class Skipped(val reason: String) : MigrationStageResult()
}

internal interface MigrationStage {
    val phase: MigrationPhase
    val rollbackOnFailure: Boolean get() = true
    fun execute(context: MigrationContext): MigrationStageResult
}
