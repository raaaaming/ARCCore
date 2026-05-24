package cc.arccore.api.di

interface Container {
    fun <T : Any> get(type: Class<T>): T
    fun <T : Any> getOrNull(type: Class<T>): T?
    fun <T : Any> register(type: Class<T>, instance: T)
    fun <T : Any> registerSingleton(type: Class<T>, instance: T)
    fun has(type: Class<*>): Boolean
    fun clear()
}
