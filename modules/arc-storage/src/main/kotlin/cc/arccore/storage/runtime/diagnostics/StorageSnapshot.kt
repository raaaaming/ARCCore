package cc.arccore.storage.runtime.diagnostics

import cc.arccore.storage.runtime.ownership.StorageOwnership
import java.time.Instant

/**
 * Point-in-time snapshot of storage runtime state.
 *
 * @property capturedAt   Timestamp when this snapshot was taken.
 * @property ownerships   All ownership records captured at snapshot time.
 * @property totalHandles Total number of tracked storage handles.
 */
data class StorageSnapshot(
    val capturedAt: Instant = Instant.now(),
    val ownerships: List<StorageOwnership>,
    val totalHandles: Int = ownerships.size
)
