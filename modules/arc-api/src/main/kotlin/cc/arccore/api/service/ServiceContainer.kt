package cc.arccore.api.service

interface ServiceContainer {

    fun <T : Any> register(serviceClass: Class<T>, service: T)

    fun <T : Any> get(serviceClass: Class<T>): T

    fun <T : Any> getOrNull(serviceClass: Class<T>): T?

    fun get(name: String): Any?

    fun <T : Any> unregister(serviceClass: Class<T>)

    fun clear()
}
