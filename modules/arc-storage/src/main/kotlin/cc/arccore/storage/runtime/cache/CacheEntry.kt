package cc.arccore.storage.runtime.cache

import java.util.concurrent.atomic.AtomicLong

/**
 * Internal wrapper holding a cached value together with access metadata.
 *
 * This class is intentionally package-private to the cache layer.
 *
 * @param V The type of the cached value.
 */
internal data class CacheEntry<V>(
    val value: V,
    val createdAt: Long = System.currentTimeMillis(),
    @Volatile var lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: AtomicLong = AtomicLong(0L)
)
