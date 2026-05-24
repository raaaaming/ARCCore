package cc.arccore.storage.runtime.cache

import cc.arccore.storage.runtime.storage.StorageHandle

/**
 * Typed in-memory (or distributed) cache storage handle.
 *
 * @param K Cache key type. Must be non-null.
 * @param V Cache value type. Must be non-null.
 */
interface CacheStorage<K : Any, V : Any> : StorageHandle {

    /**
     * Returns the cached value for [key], or `null` on a cache miss or expiry.
     */
    fun get(key: K): V?

    /**
     * Stores [value] under [key], evicting an entry if the cache is at capacity.
     */
    fun put(key: K, value: V)

    /**
     * Removes the entry for [key]. Returns `true` if an entry was present.
     */
    fun remove(key: K): Boolean

    /**
     * Returns `true` if [key] exists in the cache and has not expired.
     */
    fun contains(key: K): Boolean

    /** Returns the number of entries currently in the cache. */
    fun size(): Int

    /** Removes all entries from the cache. */
    fun clear()

    /** Returns a point-in-time snapshot of cache statistics. */
    fun stats(): CacheStats

    /**
     * Removes all entries matching [predicate].
     * Useful for targeted invalidation (e.g. all entries for a player UUID).
     */
    fun invalidateAll(predicate: (K) -> Boolean)
}
