package cc.arccore.snapshot.runtime.recovery

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryPhase
import cc.arccore.snapshot.runtime.storage.SnapshotStorageBackend

internal class LoadSnapshotStage(
    private val storage: SnapshotStorageBackend
) : RecoveryPipelineStage {
    override val phase = RecoveryPhase.LOAD_SNAPSHOT
    override val rollbackOnFailure = true

    override fun execute(context: RecoveryContext): RecoveryStageResult {
        context.phase = RecoveryPhase.LOAD_SNAPSHOT

        // snapshot은 이미 RecoveryContext에 포함되어 있으므로
        // 이 단계는 storage에서 재로드가 필요한 경우를 위한 구조
        if (context.snapshot.isEmpty()) {
            return RecoveryStageResult.Failure(
                cc.arccore.snapshot.runtime.exception.InvalidSnapshotException(
                    context.snapshot.id.value,
                    "Snapshot is empty"
                )
            )
        }
        return RecoveryStageResult.Success
    }
}
