package cc.arccore.config.runtime.metadata

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe registry of [ConfigMetadata] entries keyed by config class.
 */
class ConfigMetadataRegistry {

    private val registry: ConcurrentHashMap<KClass<*>, ConfigMetadata<*>> = ConcurrentHashMap()

    fun <T : Any> register(metadata: ConfigMetadata<T>) {
        registry[metadata.configClass] = metadata
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>): ConfigMetadata<T>? =
        registry[clazz] as? ConfigMetadata<T>

    fun registeredClasses(): Set<KClass<*>> = registry.keys.toSet()
}
