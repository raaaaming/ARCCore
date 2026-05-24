package cc.arccore.snapshot.runtime

import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleObserver
import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotId
import cc.arccore.snapshot.runtime.model.SnapshotMetrics
import cc.arccore.snapshot.runtime.recovery.RecoverableRuntime
import cc.arccore.snapshot.runtime.snapshot.SnapshotableRuntime

interface RuntimeSnapshotSystem {
    fun createSnapshot(runtimeId: String): SnapshotCaptureResult
    fun createSnapshotAll(): Map<String, SnapshotCaptureResult>

    fun recover(snapshot: RuntimeSnapshot): RecoveryResult
    fun recover(snapshotId: SnapshotId): RecoveryResult
    fun recoverLatest(runtimeId: String): RecoveryResult

    fun getLatestSnapshot(runtimeId: String): RuntimeSnapshot?
    fun getSnapshot(snapshotId: SnapshotId): RuntimeSnapshot?
    fun listSnapshots(runtimeId: String): List<SnapshotId>

    fun registerSnapshotable(runtime: SnapshotableRuntime)
    fun registerRecoverable(runtime: RecoverableRuntime)

    fun getMetrics(): SnapshotMetrics
    fun addObserver(observer: SnapshotLifecycleObserver)
    fun removeObserver(observer: SnapshotLifecycleObserver)

    // 미래 확장 포인트
    fun supportsDistributedSnapshot(): Boolean = false
    fun supportsIncrementalSnapshot(): Boolean = false
    fun supportsEventSourcedRecovery(): Boolean = false
}
