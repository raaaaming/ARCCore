package cc.arccore.config.runtime.serializer

import cc.arccore.config.runtime.config.ConfigFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe registry mapping (ConfigFormat, KClass) pairs to [ConfigSerializer] instances.
 *
 * Uses [ConcurrentHashMap.computeIfAbsent] for atomic registration and falls back to
 * [MapConfigSerializer] when no specific serializer is registered.
 */
class SerializerRegistry {

    private val registry: ConcurrentHashMap<Pair<ConfigFormat, KClass<*>>, ConfigSerializer<*>> =
        ConcurrentHashMap()

    fun <T : Any> register(format: ConfigFormat, clazz: KClass<T>, serializer: ConfigSerializer<T>) {
        registry[Pair(format, clazz)] = serializer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(format: ConfigFormat, clazz: KClass<T>): ConfigSerializer<T>? =
        registry[Pair(format, clazz)] as? ConfigSerializer<T>

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrDefault(format: ConfigFormat, clazz: KClass<T>): ConfigSerializer<T> {
        val key = Pair(format, clazz)
        return (
            registry.computeIfAbsent(key) { MapConfigSerializer<T>() }
                as ConfigSerializer<T>
            )
    }
}
