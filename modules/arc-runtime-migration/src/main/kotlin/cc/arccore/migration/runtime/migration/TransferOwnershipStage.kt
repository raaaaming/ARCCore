package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.ownership.RuntimeOwnershipRelocationCoordinator

internal class TransferOwnershipStage(
    private val ownershipCoordinator: RuntimeOwnershipRelocationCoordinator
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.TRANSFER_OWNERSHIP

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val result = ownershipCoordinator.releaseFromSource(context)
            context.transferStats.schedulerTasksRelocated = result.releasedSchedulerTasks
            context.transferStats.eventSubscriptionsRelocated = result.releasedEventSubscriptions
            context.transferStats.commandsRelocated = result.releasedCommands
            context.transferStats.ownershipTransferDurationMs = result.durationMs
            MigrationStageResult.Success
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
