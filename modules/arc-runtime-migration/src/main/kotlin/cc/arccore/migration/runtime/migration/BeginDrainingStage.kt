package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.draining.MigrationDrainCoordinator
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

internal class BeginDrainingStage(
    private val drainCoordinator: MigrationDrainCoordinator
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.BEGIN_DRAINING

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            when (val outcome = drainCoordinator.beginDrain(context)) {
                is MigrationDrainCoordinator.DrainOutcome.Completed -> MigrationStageResult.Success
                is MigrationDrainCoordinator.DrainOutcome.TimedOut ->
                    MigrationStageResult.Failure(
                        RuntimeException("Drain timed out with ${outcome.remainingInflight} inflight requests"),
                        fatalToMigration = true
                    )
                is MigrationDrainCoordinator.DrainOutcome.Forced -> MigrationStageResult.Success
            }
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
