package cc.arccore.storage.runtime.cache

import java.time.Duration

/**
 * Configures the eviction behaviour of a cache storage handle.
 *
 * @property maxSize  Maximum number of entries before eviction kicks in. Must be > 0.
 * @property ttl      Time-to-live per entry. `null` means entries never expire.
 * @property evictionStrategy Strategy applied when capacity is exceeded.
 */
data class CachePolicy(
    val maxSize: Int = 1024,
    val ttl: Duration? = null,
    val evictionStrategy: EvictionStrategy = EvictionStrategy.LRU
) {
    init {
        require(maxSize > 0) { "CachePolicy.maxSize must be > 0, got $maxSize" }
    }

    companion object {
        /** Unbounded in-memory cache with no TTL. */
        val UNBOUNDED = CachePolicy(maxSize = Int.MAX_VALUE, evictionStrategy = EvictionStrategy.NONE)

        /** 1 024-entry LRU cache with a 5-minute TTL. */
        val DEFAULT = CachePolicy(maxSize = 1024, ttl = Duration.ofMinutes(5))
    }
}
