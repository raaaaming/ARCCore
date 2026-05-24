package cc.arccore.storage.runtime.storage

/**
 * Lifecycle state of a [StorageHandle].
 */
enum class StorageHandleState {
    /** Handle is open and fully operational. */
    OPEN,
    /** Handle has been gracefully closed. */
    CLOSED,
    /** Handle has been invalidated externally (e.g. module reload). */
    STALE
}
