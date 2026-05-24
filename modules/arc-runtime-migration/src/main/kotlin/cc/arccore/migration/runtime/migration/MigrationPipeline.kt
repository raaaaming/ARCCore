package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.model.MigrationResult
import cc.arccore.migration.runtime.rollback.MigrationRollbackManager

internal class MigrationPipeline(
    private val stages: List<MigrationStage>,
    private val rollbackManager: MigrationRollbackManager,
    private val observers: List<MigrationLifecycleObserver> = emptyList()
) {
    fun execute(context: MigrationContext): MigrationResult {
        try {
            for (stage in stages) {
                if (context.abortRequested) {
                    context.phase = MigrationPhase.ABORTED
                    notifyMigrationCompleted(MigrationResult.Aborted(
                        migrationId = context.migrationId,
                        moduleId = context.moduleId,
                        phase = context.phase,
                        reason = "Abort was requested before phase ${stage.phase}"
                    ))
                    return MigrationResult.Aborted(
                        migrationId = context.migrationId,
                        moduleId = context.moduleId,
                        phase = stage.phase,
                        reason = "Abort was requested before phase ${stage.phase}"
                    )
                }

                context.phase = stage.phase
                notifyPhaseStarted(context, stage.phase)

                val result = try {
                    stage.execute(context)
                } catch (e: Exception) {
                    MigrationStageResult.Failure(error = e, fatalToMigration = true)
                }

                when (result) {
                    is MigrationStageResult.Success -> {
                        context.stageResults[stage.phase] = MigrationContext.StageOutcome.Success
                        notifyPhaseCompleted(context, stage.phase)
                    }
                    is MigrationStageResult.Skipped -> {
                        context.stageResults[stage.phase] = MigrationContext.StageOutcome.Skipped(result.reason)
                        notifyPhaseCompleted(context, stage.phase)
                    }
                    is MigrationStageResult.Failure -> {
                        context.stageResults[stage.phase] = MigrationContext.StageOutcome.Failure(result.error)
                        notifyPhaseFailed(context, stage.phase, result.error)

                        if (!result.fatalToMigration) {
                            continue
                        }

                        val rollbackSuccess = if (stage.rollbackOnFailure && context.phase.canRollback()) {
                            context.phase = MigrationPhase.ROLLING_BACK
                            notifyRollbackStarted(context, stage.phase)
                            try {
                                val rollbackResult = rollbackManager.rollback(context, stage.phase)
                                notifyRollbackCompleted(context, rollbackResult.success)
                                rollbackResult.success
                            } catch (e: Exception) {
                                notifyRollbackCompleted(context, false)
                                false
                            }
                        } else {
                            false
                        }

                        context.phase = MigrationPhase.FAILED
                        val failureResult = MigrationResult.Failure(
                            migrationId = context.migrationId,
                            moduleId = context.moduleId,
                            phase = stage.phase,
                            error = result.error,
                            rollbackSuccess = rollbackSuccess
                        )
                        notifyMigrationCompleted(failureResult)
                        return failureResult
                    }
                }
            }

            context.phase = MigrationPhase.COMPLETED
            val totalDurationMs = context.elapsedMs
            val stats = context.transferStats.toImmutable()
            val drainDurationMs = context.drainRecord.drainDurationMs
            val snapshotData = context.capturedSnapshot
            val snapshotSizeBytes = when (snapshotData) {
                is ByteArray -> snapshotData.size.toLong()
                else -> 0L
            }

            val successResult = MigrationResult.Success(
                migrationId = context.migrationId,
                moduleId = context.moduleId,
                sourceNodeId = context.sourceNodeId,
                targetNodeId = context.targetNodeId,
                totalDurationMs = totalDurationMs,
                drainDurationMs = drainDurationMs,
                snapshotSizeBytes = snapshotSizeBytes,
                transferStats = stats
            )
            notifyMigrationCompleted(successResult)
            return successResult
        } finally {
            if (!context.phase.isTerminal()) {
                context.phase = MigrationPhase.FAILED
            }
        }
    }

    private fun notifyPhaseStarted(context: MigrationContext, phase: MigrationPhase) {
        observers.forEach { runCatching { it.onPhaseStarted(context, phase) } }
    }

    private fun notifyPhaseCompleted(context: MigrationContext, phase: MigrationPhase) {
        observers.forEach { runCatching { it.onPhaseCompleted(context, phase) } }
    }

    private fun notifyPhaseFailed(context: MigrationContext, phase: MigrationPhase, error: Throwable) {
        observers.forEach { runCatching { it.onPhaseFailed(context, phase, error) } }
    }

    private fun notifyMigrationCompleted(result: MigrationResult) {
        observers.forEach { runCatching { it.onMigrationCompleted(result) } }
    }

    private fun notifyRollbackStarted(context: MigrationContext, failedPhase: MigrationPhase) {
        observers.forEach { runCatching { it.onRollbackStarted(context, failedPhase) } }
    }

    private fun notifyRollbackCompleted(context: MigrationContext, success: Boolean) {
        observers.forEach { runCatching { it.onRollbackCompleted(context, success) } }
    }
}
