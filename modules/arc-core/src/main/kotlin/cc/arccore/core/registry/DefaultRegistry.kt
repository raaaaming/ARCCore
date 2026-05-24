package cc.arccore.core.registry

import cc.arccore.api.registry.Registry
import cc.arccore.api.registry.RegistryKey
import java.util.concurrent.ConcurrentHashMap

class DefaultRegistry : Registry {

    private val map = ConcurrentHashMap<RegistryKey, Any>()

    override fun <T : Any> register(key: RegistryKey, value: T) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: RegistryKey): T? = map[key] as? T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOrNull(key: RegistryKey): T? = map[key] as? T

    override fun has(key: RegistryKey): Boolean = map.containsKey(key)

    override fun keys(): Set<RegistryKey> = map.keys.toSet()

    override fun clear() {
        map.clear()
    }
}
