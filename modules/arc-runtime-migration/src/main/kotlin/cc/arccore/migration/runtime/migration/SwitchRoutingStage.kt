package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.coordination.MigrationRoutingSwitch
import cc.arccore.migration.runtime.exception.RoutingSwitchException
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase

internal class SwitchRoutingStage(
    private val routingSwitch: MigrationRoutingSwitch
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.SWITCH_ROUTING

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            when (val outcome = routingSwitch.switchToTarget(context)) {
                is MigrationRoutingSwitch.RoutingSwitchOutcome.Switched -> {
                    context.rollbackAvailable = false
                    MigrationStageResult.Success
                }
                is MigrationRoutingSwitch.RoutingSwitchOutcome.Failed ->
                    MigrationStageResult.Failure(
                        RoutingSwitchException(context.migrationId, "Routing switch failed: ${outcome.error.message}", outcome.error)
                    )
            }
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
