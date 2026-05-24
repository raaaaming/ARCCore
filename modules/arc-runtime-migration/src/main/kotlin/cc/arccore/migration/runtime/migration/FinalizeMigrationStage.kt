package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.ownership.RuntimeOwnershipRelocationCoordinator
import cc.arccore.migration.runtime.state.MigrationSessionRegistry

internal class FinalizeMigrationStage(
    private val ownershipCoordinator: RuntimeOwnershipRelocationCoordinator,
    private val sessionRegistry: MigrationSessionRegistry
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.FINALIZE_MIGRATION

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val assignResult = ownershipCoordinator.assignToTarget(context)
            context.transferStats.schedulerTasksRelocated += assignResult.assignedSchedulerTasks
            context.transferStats.eventSubscriptionsRelocated += assignResult.assignedEventSubscriptions
            context.transferStats.commandsRelocated += assignResult.assignedCommands
            sessionRegistry.complete(context.migrationId)
            MigrationStageResult.Success
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
