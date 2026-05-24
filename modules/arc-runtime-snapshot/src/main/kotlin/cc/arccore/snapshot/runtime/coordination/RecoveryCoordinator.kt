package cc.arccore.snapshot.runtime.coordination

import cc.arccore.snapshot.runtime.model.RecoveryContext
import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotId
import cc.arccore.snapshot.runtime.ownership.OwnershipRecoveryManager
import cc.arccore.snapshot.runtime.recovery.FinalizeRecoveryStage
import cc.arccore.snapshot.runtime.recovery.LoadSnapshotStage
import cc.arccore.snapshot.runtime.recovery.PrepareRecoveryStage
import cc.arccore.snapshot.runtime.recovery.RecoverableRuntime
import cc.arccore.snapshot.runtime.recovery.RecoveryPipeline
import cc.arccore.snapshot.runtime.recovery.RestoreOwnershipStage
import cc.arccore.snapshot.runtime.recovery.RestoreRuntimeStateStage
import cc.arccore.snapshot.runtime.recovery.ValidateSnapshotStage
import cc.arccore.snapshot.runtime.snapshot.SnapshotRegistry
import cc.arccore.snapshot.runtime.state.RecoverySessionRegistry
import cc.arccore.snapshot.runtime.storage.SnapshotStorageBackend
import cc.arccore.snapshot.runtime.validation.SnapshotValidator
import java.util.concurrent.ConcurrentHashMap

internal class RecoveryCoordinator(
    private val snapshotRegistry: SnapshotRegistry,
    private val storage: SnapshotStorageBackend,
    private val sessionRegistry: RecoverySessionRegistry,
    private val ownershipManager: OwnershipRecoveryManager
) {
    private val recoverableRuntimes = ConcurrentHashMap<String, RecoverableRuntime>()

    fun register(runtime: RecoverableRuntime) {
        recoverableRuntimes[runtime.runtimeId] = runtime
        ownershipManager.register(runtime)
    }

    fun recover(snapshot: RuntimeSnapshot): RecoveryResult {
        val context = RecoveryContext(
            targetRuntimeId = snapshot.runtimeId,
            snapshot = snapshot
        )

        val pipeline = RecoveryPipeline(
            stages = listOf(
                PrepareRecoveryStage(sessionRegistry, SnapshotValidator()),
                LoadSnapshotStage(storage),
                ValidateSnapshotStage(SnapshotValidator()),
                RestoreOwnershipStage(ownershipManager),
                RestoreRuntimeStateStage(recoverableRuntimes),
                FinalizeRecoveryStage(sessionRegistry)
            )
        )

        return pipeline.execute(context)
    }

    fun recover(snapshotId: SnapshotId): RecoveryResult {
        val snapshot = storage.load(snapshotId)
            ?: snapshotRegistry.get(snapshotId)
            ?: return RecoveryResult.Rejected("unknown", "Snapshot not found: $snapshotId")
        return recover(snapshot)
    }

    fun recoverLatest(runtimeId: String): RecoveryResult {
        val snapshot = snapshotRegistry.getLatest(runtimeId)
            ?: return RecoveryResult.Rejected(runtimeId, "No snapshot found for '$runtimeId'")
        return recover(snapshot)
    }
}
