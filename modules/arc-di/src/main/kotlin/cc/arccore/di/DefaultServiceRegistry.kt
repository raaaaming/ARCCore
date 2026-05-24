package cc.arccore.di

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.service.ServiceDescriptor
import cc.arccore.api.service.ServiceRegistry
import cc.arccore.api.service.exception.DuplicateServiceException
import cc.arccore.api.service.exception.ServiceNotFoundException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class DefaultServiceRegistry : ServiceRegistry {

    private val entries = ConcurrentHashMap<KClass<*>, ServiceDescriptor<*>>()

    override fun <T : Any> register(
        type: KClass<T>,
        provider: T,
        owner: ModuleContainerView,
        override: Boolean
    ) {
        if (override) {
            entries[type] = ServiceDescriptor.of(type, owner, provider)
        } else {
            val existing = entries.putIfAbsent(type, ServiceDescriptor.of(type, owner, provider))
            if (existing != null) {
                throw DuplicateServiceException(type, existing.ownerId)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KClass<T>): T? =
        (entries[type]?.implementation as? T)

    override fun <T : Any> require(type: KClass<T>): T =
        get(type) ?: throw ServiceNotFoundException(type)

    override fun <T : Any> unregister(type: KClass<T>) {
        entries.remove(type)
    }

    override fun unregisterAll(owner: ModuleContainerView): Int =
        unregisterAllById(owner.module.id)

    override fun unregisterAllById(ownerId: String): Int {
        var count = 0
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.ownerId == ownerId) {
                iterator.remove()
                count++
            }
        }
        return count
    }

    override fun <T : Any> isRegistered(type: KClass<T>): Boolean = entries.containsKey(type)

    override fun registeredTypes(): Set<KClass<*>> = entries.keys.toSet()

    override fun registeredBy(owner: ModuleContainerView): Set<KClass<*>> =
        registeredById(owner.module.id)

    override fun registeredById(ownerId: String): Set<KClass<*>> =
        entries.entries
            .filter { it.value.ownerId == ownerId }
            .mapTo(mutableSetOf()) { it.key }
}
