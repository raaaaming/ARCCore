package cc.arccore.snapshot.runtime

import cc.arccore.snapshot.runtime.coordination.RecoveryCoordinator
import cc.arccore.snapshot.runtime.coordination.SnapshotCoordinator
import cc.arccore.snapshot.runtime.diagnostics.SnapshotDiagnosticsCollector
import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleEvent
import cc.arccore.snapshot.runtime.lifecycle.SnapshotLifecycleObserver
import cc.arccore.snapshot.runtime.metrics.SnapshotMetricsAccumulator
import cc.arccore.snapshot.runtime.model.RecoveryResult
import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotId
import cc.arccore.snapshot.runtime.model.SnapshotMetrics
import cc.arccore.snapshot.runtime.ownership.OwnershipRecoveryManager
import cc.arccore.snapshot.runtime.recovery.RecoverableRuntime
import cc.arccore.snapshot.runtime.serialization.MapStateSerializer
import cc.arccore.snapshot.runtime.snapshot.SnapshotCapture
import cc.arccore.snapshot.runtime.snapshot.SnapshotRegistry
import cc.arccore.snapshot.runtime.snapshot.SnapshotableRuntime
import cc.arccore.snapshot.runtime.state.RecoverySessionRegistry
import cc.arccore.snapshot.runtime.storage.InMemorySnapshotStorage
import java.util.concurrent.CopyOnWriteArrayList

class DefaultRuntimeSnapshotSystem : RuntimeSnapshotSystem {
    private val storage = InMemorySnapshotStorage()
    private val registry = SnapshotRegistry()
    private val sessionRegistry = RecoverySessionRegistry()
    private val ownershipManager = OwnershipRecoveryManager()
    private val metricsAccumulator = SnapshotMetricsAccumulator()
    private val diagnostics = SnapshotDiagnosticsCollector(metricsAccumulator)
    private val observers = CopyOnWriteArrayList<SnapshotLifecycleObserver>()

    private val snapshotCoordinator = SnapshotCoordinator(
        capture = SnapshotCapture(),
        registry = registry,
        storage = storage
    )

    private val recoveryCoordinator = RecoveryCoordinator(
        snapshotRegistry = registry,
        storage = storage,
        sessionRegistry = sessionRegistry,
        ownershipManager = ownershipManager
    )

    init {
        observers.add(diagnostics)
    }

    override fun createSnapshot(runtimeId: String): SnapshotCaptureResult {
        val result = snapshotCoordinator.captureSnapshot(runtimeId)
        diagnostics.recordCapture(result)

        if (result is SnapshotCaptureResult.Success) {
            notifyObservers(SnapshotLifecycleEvent.SnapshotCaptured(
                runtimeId = runtimeId,
                snapshotId = result.snapshot.id,
                captureDurationMs = result.captureDurationMs,
                sizeBytes = result.snapshot.metadata.sizeBytes
            ))
        } else if (result is SnapshotCaptureResult.Failure) {
            notifyObservers(SnapshotLifecycleEvent.SnapshotCaptureFailed(runtimeId, result.error))
        }

        return result
    }

    override fun createSnapshotAll(): Map<String, SnapshotCaptureResult> =
        snapshotCoordinator.captureAll().also { results ->
            results.forEach { (_, result) -> diagnostics.recordCapture(result) }
        }

    override fun recover(snapshot: RuntimeSnapshot): RecoveryResult {
        val result = recoveryCoordinator.recover(snapshot)
        diagnostics.recordRecovery(result)
        return result
    }

    override fun recover(snapshotId: SnapshotId): RecoveryResult {
        val result = recoveryCoordinator.recover(snapshotId)
        diagnostics.recordRecovery(result)
        return result
    }

    override fun recoverLatest(runtimeId: String): RecoveryResult {
        val result = recoveryCoordinator.recoverLatest(runtimeId)
        diagnostics.recordRecovery(result)
        return result
    }

    override fun getLatestSnapshot(runtimeId: String): RuntimeSnapshot? =
        registry.getLatest(runtimeId)

    override fun getSnapshot(snapshotId: SnapshotId): RuntimeSnapshot? =
        storage.load(snapshotId)

    override fun listSnapshots(runtimeId: String): List<SnapshotId> =
        storage.listByRuntime(runtimeId)

    override fun registerSnapshotable(runtime: SnapshotableRuntime) {
        snapshotCoordinator.register(runtime)
    }

    override fun registerRecoverable(runtime: RecoverableRuntime) {
        recoveryCoordinator.register(runtime)
    }

    override fun getMetrics(): SnapshotMetrics = metricsAccumulator.snapshot()

    override fun addObserver(observer: SnapshotLifecycleObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: SnapshotLifecycleObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers(event: SnapshotLifecycleEvent) {
        observers.forEach { it.onSnapshotEvent(event) }
    }
}
