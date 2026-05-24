package cc.arccore.snapshot.runtime.model

sealed class SnapshotCaptureResult {
    data class Success(
        val snapshot: RuntimeSnapshot,
        val captureDurationMs: Long
    ) : SnapshotCaptureResult()

    data class Failure(
        val runtimeId: String,
        val error: Throwable
    ) : SnapshotCaptureResult()

    data class Unsupported(
        val runtimeId: String,
        val reason: String = "Runtime does not implement SnapshotableRuntime"
    ) : SnapshotCaptureResult()
}
