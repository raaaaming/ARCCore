package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.validation.SnapshotValidator

internal class ValidateSnapshotStage(
    private val validator: SnapshotValidator,
    private val maxSnapshotAgeMs: Long = 30 * 60 * 1000L
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.VALIDATE_SNAPSHOT
    override val rollbackOnFailure = true

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.VALIDATE_SNAPSHOT

        val snapshot = context.snapshot
        val ageMs = snapshot.metadata.ageMs()

        if (ageMs > maxSnapshotAgeMs) {
            return RecoveryStageResult.Failure(
                cc.arccore.snapshot.runtime.exception.StaleSnapshotException(
                    snapshot.id.value, ageMs, maxSnapshotAgeMs
                )
            )
        }

        if (snapshot.runtimeId != context.targetRuntimeId) {
            return RecoveryStageResult.Failure(
                cc.arccore.snapshot.runtime.exception.InvalidSnapshotException(
                    snapshot.id.value,
                    "Snapshot runtimeId '${snapshot.runtimeId}' does not match target '${context.targetRuntimeId}'"
                )
            )
        }

        return RecoveryStageResult.Success
    }
}
