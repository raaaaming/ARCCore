package cc.arccore.snapshot.runtime.snapshot

import cc.arccore.snapshot.runtime.model.RuntimeSnapshot
import cc.arccore.snapshot.runtime.model.RuntimeSnapshotMetadata
import cc.arccore.snapshot.runtime.model.SnapshotCaptureResult
import cc.arccore.snapshot.runtime.model.SnapshotId
import java.time.Instant

internal class SnapshotCapture {
    fun capture(runtime: SnapshotableRuntime): SnapshotCaptureResult {
        val startMs = System.currentTimeMillis()
        return try {
            val state = runtime.captureSnapshot().state
            val ownershipState = if (runtime.supportsOwnershipSnapshot()) {
                runtime.captureOwnershipSnapshot()
            } else emptyMap()

            val now = Instant.now()
            val snapshotId = SnapshotId.of(runtime.runtimeId, now.toEpochMilli())

            val snapshot = RuntimeSnapshot(
                id = snapshotId,
                runtimeId = runtime.runtimeId,
                capturedAt = now,
                state = state,
                ownershipState = ownershipState,
                metadata = RuntimeSnapshotMetadata(
                    snapshotId = snapshotId,
                    runtimeId = runtime.runtimeId,
                    runtimeType = runtime.runtimeType,
                    capturedAt = now,
                    sizeBytes = estimateSizeBytes(state, ownershipState),
                    ownershipIncluded = ownershipState.isNotEmpty()
                )
            )

            SnapshotCaptureResult.Success(snapshot, System.currentTimeMillis() - startMs)
        } catch (e: Exception) {
            SnapshotCaptureResult.Failure(runtime.runtimeId, e)
        }
    }

    private fun estimateSizeBytes(
        state: Map<String, Any?>,
        ownershipState: Map<String, Any?>
    ): Long = ((state.size + ownershipState.size) * 64L)
}
