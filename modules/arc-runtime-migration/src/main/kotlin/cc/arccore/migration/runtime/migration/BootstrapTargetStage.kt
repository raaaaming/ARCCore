package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.exception.TargetBootstrapException
import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.transport.RuntimeRelocationTransport
import cc.arccore.migration.runtime.transport.TransportTransferResult
import cc.arccore.migration.runtime.transfer.TargetNodeBootstrapper

internal class BootstrapTargetStage(
    private val transport: RuntimeRelocationTransport,
    private val targetBootstrapper: TargetNodeBootstrapper
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.BOOTSTRAP_TARGET

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val snapshotData = (context.capturedSnapshot as? ByteArray) ?: ByteArray(0)
            val snapshotId = context.migrationId.value
            val transferResult = transport.transferSnapshot(context.targetNodeId, snapshotData, context.moduleId, snapshotId)
            if (transferResult is TransportTransferResult.Failure) {
                return MigrationStageResult.Failure(transferResult.error)
            }
            when (val bootstrapResult = targetBootstrapper.bootstrap(context)) {
                is TargetNodeBootstrapper.BootstrapOutcome.Success -> {
                    when (val readinessResult = targetBootstrapper.awaitReady(context.moduleId, context.targetNodeId)) {
                        is TargetNodeBootstrapper.ReadinessOutcome.Ready -> MigrationStageResult.Success
                        is TargetNodeBootstrapper.ReadinessOutcome.TimedOut ->
                            MigrationStageResult.Failure(
                                TargetBootstrapException(context.migrationId, context.targetNodeId, "Target readiness timed out for module '${context.moduleId}'")
                            )
                        is TargetNodeBootstrapper.ReadinessOutcome.Error ->
                            MigrationStageResult.Failure(readinessResult.error)
                    }
                }
                is TargetNodeBootstrapper.BootstrapOutcome.Failure ->
                    MigrationStageResult.Failure(bootstrapResult.error)
            }
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
