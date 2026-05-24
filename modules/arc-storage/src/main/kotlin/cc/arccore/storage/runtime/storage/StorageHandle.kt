package cc.arccore.storage.runtime.storage

import java.util.UUID

/**
 * Base interface for all storage handles.
 *
 * A handle represents a single scoped resource (config, file, cache, database)
 * owned by a specific module. Once [close] is called the handle transitions to
 * [StorageHandleState.CLOSED] and must no longer be accessed.
 */
interface StorageHandle : AutoCloseable {

    /** Unique identifier for this handle instance. */
    val handleId: UUID

    /** The module identifier that owns this handle. */
    val moduleId: String

    /** The category of storage resource this handle wraps. */
    val storageType: StorageType

    /** Current lifecycle state of this handle. */
    val state: StorageHandleState

    /** Closes this handle and releases all associated resources. Idempotent. */
    override fun close()
}
