package cc.arccore.storage.runtime.cache

/**
 * Snapshot of cache performance counters.
 *
 * @property hitCount     Number of successful cache lookups.
 * @property missCount    Number of failed cache lookups (key absent or expired).
 * @property evictionCount Number of entries evicted due to capacity or TTL.
 * @property currentSize  Number of entries currently in the cache.
 */
data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val evictionCount: Long,
    val currentSize: Int
) {
    /** Hit rate in the range [0.0, 1.0], or 0.0 if no requests have been made. */
    val hitRate: Double
        get() {
            val total = hitCount + missCount
            return if (total == 0L) 0.0 else hitCount.toDouble() / total
        }
}
