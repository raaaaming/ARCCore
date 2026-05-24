package cc.arccore.storage.runtime.cache

import cc.arccore.storage.runtime.storage.AbstractStorageHandle
import cc.arccore.storage.runtime.storage.StorageType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe in-memory [CacheStorage] with optional TTL expiry and size-based eviction.
 *
 * A single-threaded daemon executor handles periodic TTL cleanup when [CachePolicy.ttl]
 * is configured. Eviction on capacity overflow applies the strategy from [CachePolicy.evictionStrategy].
 *
 * @param K Cache key type.
 * @param V Cache value type.
 */
class InMemoryCacheStorage<K : Any, V : Any>(
    override val moduleId: String,
    private val name: String,
    private val policy: CachePolicy
) : AbstractStorageHandle(moduleId = moduleId, storageType = StorageType.CACHE), CacheStorage<K, V> {

    private val store = ConcurrentHashMap<K, CacheEntry<V>>()
    private val _hitCount = AtomicLong(0L)
    private val _missCount = AtomicLong(0L)
    private val _evictionCount = AtomicLong(0L)

    private val scheduler: ScheduledExecutorService? = if (policy.ttl != null) {
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "arc-cache-evict-$name").apply { isDaemon = true }
        }.also { exec ->
            exec.scheduleAtFixedRate(::evictExpired, 10L, 10L, TimeUnit.SECONDS)
        }
    } else null

    override fun get(key: K): V? {
        checkOpen()
        val entry = store[key] ?: run {
            _missCount.incrementAndGet()
            return null
        }
        if (isExpired(entry)) {
            store.remove(key)
            _evictionCount.incrementAndGet()
            _missCount.incrementAndGet()
            return null
        }
        entry.lastAccessedAt = System.currentTimeMillis()
        entry.accessCount.incrementAndGet()
        _hitCount.incrementAndGet()
        return entry.value
    }

    override fun put(key: K, value: V) {
        checkOpen()
        if (store.size >= policy.maxSize && !store.containsKey(key)) {
            evictOne()
        }
        store[key] = CacheEntry(value)
    }

    override fun remove(key: K): Boolean {
        checkOpen()
        return store.remove(key) != null
    }

    override fun contains(key: K): Boolean {
        checkOpen()
        val entry = store[key] ?: return false
        if (isExpired(entry)) {
            store.remove(key)
            _evictionCount.incrementAndGet()
            return false
        }
        return true
    }

    override fun size(): Int {
        checkOpen()
        return store.size
    }

    override fun clear() {
        checkOpen()
        store.clear()
    }

    override fun stats(): CacheStats = CacheStats(
        hitCount = _hitCount.get(),
        missCount = _missCount.get(),
        evictionCount = _evictionCount.get(),
        currentSize = store.size
    )

    override fun invalidateAll(predicate: (K) -> Boolean) {
        checkOpen()
        store.keys.filter(predicate).forEach { store.remove(it) }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private fun isExpired(entry: CacheEntry<V>): Boolean {
        val ttl = policy.ttl ?: return false
        return System.currentTimeMillis() - entry.createdAt > ttl.toMillis()
    }

    private fun evictOne() {
        when (policy.evictionStrategy) {
            EvictionStrategy.LRU -> store.minByOrNull { it.value.lastAccessedAt }?.key?.let {
                store.remove(it)
                _evictionCount.incrementAndGet()
            }
            EvictionStrategy.LFU -> store.minByOrNull { it.value.accessCount.get() }?.key?.let {
                store.remove(it)
                _evictionCount.incrementAndGet()
            }
            EvictionStrategy.FIFO -> store.minByOrNull { it.value.createdAt }?.key?.let {
                store.remove(it)
                _evictionCount.incrementAndGet()
            }
            EvictionStrategy.NONE -> { /* Drop the incoming entry silently. */ }
        }
    }

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        val ttlMs = policy.ttl?.toMillis() ?: return
        store.entries.removeIf { (_, entry) ->
            (now - entry.createdAt > ttlMs).also { evicted ->
                if (evicted) _evictionCount.incrementAndGet()
            }
        }
    }

    override fun onClose() {
        scheduler?.shutdown()
        store.clear()
    }
}
