package cc.arccore.runtime.context.access

import kotlin.reflect.KClass

interface ScopedServiceAccess {
    fun <T : Any> register(type: KClass<T>, provider: T, override: Boolean = false)
    fun <T : Any> get(type: KClass<T>): T?
    fun <T : Any> require(type: KClass<T>): T
    fun <T : Any> unregister(type: KClass<T>)
    fun unregisterAll(): Int
    fun registeredTypes(): Set<KClass<*>>
}
