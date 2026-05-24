package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.coordination.MigrationNodeRegistry
import cc.arccore.migration.runtime.exception.MigrationAlreadyActiveException
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.state.MigrationSessionRegistry

internal class PrepareMigrationStage(
    private val sessionRegistry: MigrationSessionRegistry,
    private val nodeRegistry: MigrationNodeRegistry
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.PREPARE_MIGRATION

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            if (sessionRegistry.isModuleMigrating(context.moduleId)) {
                return MigrationStageResult.Failure(MigrationAlreadyActiveException(context.moduleId))
            }
            if (!nodeRegistry.exists(context.sourceNodeId)) {
                return MigrationStageResult.Failure(IllegalStateException("Source node '${context.sourceNodeId}' not found"))
            }
            if (!nodeRegistry.exists(context.targetNodeId)) {
                return MigrationStageResult.Failure(IllegalStateException("Target node '${context.targetNodeId}' not found"))
            }
            if (context.sourceNodeId == context.targetNodeId) {
                return MigrationStageResult.Failure(IllegalArgumentException("Source and target node cannot be the same: '${context.sourceNodeId}'"))
            }
            MigrationStageResult.Success
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
