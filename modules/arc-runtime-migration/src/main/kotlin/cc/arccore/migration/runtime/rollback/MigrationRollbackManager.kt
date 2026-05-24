package cc.arccore.migration.runtime.rollback

import cc.arccore.migration.runtime.draining.MigrationDrainCoordinator
import cc.arccore.migration.runtime.exception.MigrationRollbackException
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.ownership.RuntimeOwnershipRelocationCoordinator

internal class MigrationRollbackManager(
    private val drainCoordinator: MigrationDrainCoordinator,
    private val ownershipCoordinator: RuntimeOwnershipRelocationCoordinator
) {
    fun rollback(context: MigrationContext, failedPhase: MigrationPhase): MigrationRollbackResult {
        return when (failedPhase) {
            MigrationPhase.PREPARE_MIGRATION -> rollbackFromPrepare(context)
            MigrationPhase.VALIDATE_TARGET -> rollbackFromPrepare(context)
            MigrationPhase.BEGIN_DRAINING -> rollbackFromBeginDraining(context)
            MigrationPhase.SNAPSHOT_STATE -> rollbackFromBeginDraining(context)
            MigrationPhase.TRANSFER_OWNERSHIP -> rollbackFromTransferOwnership(context)
            MigrationPhase.BOOTSTRAP_TARGET -> rollbackFromBootstrapTarget(context)
            MigrationPhase.RESTORE_STATE -> rollbackFromTransferOwnership(context)
            MigrationPhase.SWITCH_ROUTING -> throw MigrationRollbackException(
                migrationId = context.migrationId,
                originalError = IllegalStateException("Routing already switched, rollback is not possible"),
                message = "Cannot rollback after SWITCH_ROUTING for migration ${context.migrationId}"
            )
            MigrationPhase.FINALIZE_MIGRATION,
            MigrationPhase.CLEANUP_SOURCE,
            MigrationPhase.COMPLETED,
            MigrationPhase.ROLLING_BACK,
            MigrationPhase.ABORTED,
            MigrationPhase.FAILED,
            MigrationPhase.IDLE -> MigrationRollbackResult(success = false)
        }
    }

    private fun rollbackFromPrepare(context: MigrationContext): MigrationRollbackResult {
        return MigrationRollbackResult(success = true)
    }

    private fun rollbackFromBeginDraining(context: MigrationContext): MigrationRollbackResult {
        val failedSteps = mutableListOf<Pair<String, Throwable>>()
        var drainReleased = false
        try {
            drainCoordinator.releaseDrain(context)
            drainReleased = true
        } catch (e: Exception) {
            failedSteps += "releaseDrain" to e
        }
        return MigrationRollbackResult(
            success = failedSteps.isEmpty(),
            drainReleased = drainReleased,
            failedSteps = failedSteps
        )
    }

    private fun rollbackFromTransferOwnership(context: MigrationContext): MigrationRollbackResult {
        val failedSteps = mutableListOf<Pair<String, Throwable>>()
        var ownershipRestored = false
        var drainReleased = false
        try {
            ownershipCoordinator.rollbackToSource(context)
            ownershipRestored = true
        } catch (e: Exception) {
            failedSteps += "ownershipRollbackToSource" to e
        }
        try {
            drainCoordinator.releaseDrain(context)
            drainReleased = true
        } catch (e: Exception) {
            failedSteps += "releaseDrain" to e
        }
        return MigrationRollbackResult(
            success = failedSteps.isEmpty(),
            drainReleased = drainReleased,
            ownershipRestoredToSource = ownershipRestored,
            failedSteps = failedSteps
        )
    }

    private fun rollbackFromBootstrapTarget(context: MigrationContext): MigrationRollbackResult {
        val failedSteps = mutableListOf<Pair<String, Throwable>>()
        var ownershipRestored = false
        var drainReleased = false
        try {
            ownershipCoordinator.rollbackToSource(context)
            ownershipRestored = true
        } catch (e: Exception) {
            failedSteps += "ownershipRollbackToSource" to e
        }
        try {
            drainCoordinator.releaseDrain(context)
            drainReleased = true
        } catch (e: Exception) {
            failedSteps += "releaseDrain" to e
        }
        return MigrationRollbackResult(
            success = failedSteps.isEmpty(),
            drainReleased = drainReleased,
            ownershipRestoredToSource = ownershipRestored,
            targetBootstrapCleaned = false,
            failedSteps = failedSteps
        )
    }
}
