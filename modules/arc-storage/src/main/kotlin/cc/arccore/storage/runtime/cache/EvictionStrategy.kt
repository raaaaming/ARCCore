package cc.arccore.storage.runtime.cache

/**
 * Determines which entry is evicted when the cache reaches its maximum size.
 */
enum class EvictionStrategy {
    /** Evict the least recently accessed entry. */
    LRU,
    /** Evict the least frequently accessed entry. */
    LFU,
    /** Evict the oldest inserted entry (first-in, first-out). */
    FIFO,
    /** Never evict — new entries are dropped when capacity is reached. */
    NONE
}
