package cc.arccore.api.registry

interface Registry {
    fun <T : Any> register(key: RegistryKey, value: T)
    fun <T : Any> get(key: RegistryKey): T?
    fun <T : Any> getOrNull(key: RegistryKey): T?
    fun has(key: RegistryKey): Boolean
    fun keys(): Set<RegistryKey>
    fun clear()
}
