package cc.arccore.snapshot.runtime.coordination

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotId
import cc.arccore.snapshot.runtime.snapshot.SnapshotCapture
import cc.arccore.snapshot.runtime.snapshot.SnapshotRegistry
import cc.arccore.snapshot.runtime.snapshot.SnapshotableRuntime
import cc.arccore.snapshot.runtime.storage.SnapshotStorageBackend
import java.util.concurrent.ConcurrentHashMap

internal class SnapshotCoordinator(
    private val capture: SnapshotCapture,
    private val registry: SnapshotRegistry,
    private val storage: SnapshotStorageBackend
) {
    private val snapshotableRuntimes = ConcurrentHashMap<String, SnapshotableRuntime>()

    fun register(runtime: SnapshotableRuntime) {
        snapshotableRuntimes[runtime.runtimeId] = runtime
    }

    fun unregister(runtimeId: String) {
        snapshotableRuntimes.remove(runtimeId)
    }

    fun captureSnapshot(runtimeId: String): SnapshotCaptureResult {
        val runtime = snapshotableRuntimes[runtimeId]
            ?: return SnapshotCaptureResult.Unsupported(runtimeId)

        val result = capture.capture(runtime)
        if (result is SnapshotCaptureResult.Success) {
            registry.store(result.snapshot)
            storage.store(result.snapshot)
        }
        return result
    }

    fun captureAll(): Map<String, SnapshotCaptureResult> =
        snapshotableRuntimes.keys.associateWith { captureSnapshot(it) }

    fun getLatestSnapshot(runtimeId: String): RuntimeSnapshot? = registry.getLatest(runtimeId)

    fun getSnapshot(snapshotId: SnapshotId): RuntimeSnapshot? = storage.load(snapshotId)

    fun registeredRuntimes(): Set<String> = snapshotableRuntimes.keys.toSet()
}
