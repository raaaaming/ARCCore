package cc.arccore.di.generated.injector.singleton

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe cache for generated singleton instances.
 *
 * Uses [ConcurrentHashMap.computeIfAbsent] to guarantee at-most-once creation
 * under concurrent access without synchronization on the happy path.
 *
 * [clearWithTypes] returns the cleared entries so callers can invoke
 * [cc.arccore.api.di.generated.GeneratedInjector.cleanup] on each instance
 * during module unload.
 */
class SingletonCache {

    private val cache = ConcurrentHashMap<KClass<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrCreate(type: KClass<T>, factory: () -> T): T =
        cache.computeIfAbsent(type) { factory() } as T

    fun contains(type: KClass<*>): Boolean = cache.containsKey(type)

    fun clearWithTypes(): Map<KClass<*>, Any> {
        val snapshot = HashMap(cache)
        cache.clear()
        return snapshot
    }

    val size: Int get() = cache.size
}
