package cc.arccore.migration.runtime.migration

import cc.arccore.migration.runtime.model.MigrationContext
import cc.arccore.migration.runtime.model.MigrationPhase
import cc.arccore.migration.runtime.transport.RuntimeRelocationTransport
import cc.arccore.migration.runtime.validation.MigrationTargetValidator
import cc.arccore.migration.runtime.validation.MigrationValidationFailure

internal class ValidateTargetStage(
    private val targetValidator: MigrationTargetValidator,
    private val transport: RuntimeRelocationTransport
) : MigrationStage {
    override val phase: MigrationPhase = MigrationPhase.VALIDATE_TARGET

    override fun execute(context: MigrationContext): MigrationStageResult {
        return try {
            val failures = targetValidator.validate(context)
            if (failures.isEmpty()) {
                MigrationStageResult.Success
            } else {
                val first = failures.first()
                val error = when (first) {
                    is MigrationValidationFailure.TargetNodeNotFound ->
                        IllegalStateException("Target node not found: ${first.nodeId}")
                    is MigrationValidationFailure.TargetNodeAtCapacity ->
                        IllegalStateException("Target node at capacity: ${first.nodeId} (max: ${first.capacity})")
                    is MigrationValidationFailure.SourceAlreadyMigrating ->
                        IllegalStateException("Source already migrating: ${first.moduleId}")
                    is MigrationValidationFailure.TransportUnreachable ->
                        IllegalStateException("Transport unreachable: ${first.targetNodeId}")
                    is MigrationValidationFailure.SnapshotUnavailable ->
                        IllegalStateException("Snapshot unavailable")
                    is MigrationValidationFailure.VersionIncompatible ->
                        IllegalStateException("Version incompatible: ${first.sourceVersion} vs ${first.targetVersion}")
                    is MigrationValidationFailure.GenericValidationFailed ->
                        IllegalStateException("Validation failed: ${first.reason}")
                }
                MigrationStageResult.Failure(error)
            }
        } catch (e: Exception) {
            MigrationStageResult.Failure(e)
        }
    }
}
