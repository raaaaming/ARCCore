package cc.arccore.migration.runtime.model

import java.util.concurrent.ConcurrentHashMap

internal class MigrationContext(
    val migrationId: MigrationId,
    val moduleId: String,
    val sourceNodeId: String,
    val targetNodeId: String
) {
    val startTimeMs: Long = System.currentTimeMillis()

    @Volatile var phase: MigrationPhase = MigrationPhase.IDLE
    @Volatile var capturedSnapshot: Any? = null
    @Volatile var restoreResult: Any? = null
    @Volatile var rollbackAvailable: Boolean = true
    @Volatile var abortRequested: Boolean = false

    val transferStats: MutableMigrationTransferStats = MutableMigrationTransferStats()
    val drainRecord: MigrationDrainRecord = MigrationDrainRecord()
    val stageResults: ConcurrentHashMap<MigrationPhase, StageOutcome> = ConcurrentHashMap()

    val elapsedMs: Long get() = System.currentTimeMillis() - startTimeMs

    sealed class StageOutcome {
        data object Success : StageOutcome()
        data class Failure(val error: Throwable) : StageOutcome()
        data class Skipped(val reason: String) : StageOutcome()
    }
}
