package cc.arccore.storage.runtime.ownership

import cc.arccore.storage.runtime.storage.StorageType
import java.time.Instant
import java.util.UUID

/**
 * Records which module owns a particular storage handle.
 *
 * @property moduleId   The owning module's identifier.
 * @property handleId   The unique identifier of the storage handle.
 * @property storageType The kind of storage resource.
 * @property registeredAt Timestamp when this ownership record was created.
 */
data class StorageOwnership(
    val moduleId: String,
    val handleId: UUID,
    val storageType: StorageType,
    val registeredAt: Instant = Instant.now()
)
