package cc.arccore.runtime.context.access

import cc.arccore.api.module.CleanupScope
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.service.ServiceRegistry
import cc.arccore.api.service.exception.ServiceNotFoundException
import kotlin.reflect.KClass

class DefaultScopedServiceAccess(
    private val serviceRegistry: ServiceRegistry,
    private val owner: ModuleContainerView,
    private val cleanupScope: CleanupScope,
    private val autoCleanup: Boolean = true
) : ScopedServiceAccess {

    override fun <T : Any> register(type: KClass<T>, provider: T, override: Boolean) {
        serviceRegistry.register(type, provider, owner, override)
        if (autoCleanup) {
            val key = type.qualifiedName ?: type.java.name
            cleanupScope.register(key, AutoCloseable { serviceRegistry.unregister(type) })
        }
    }

    override fun <T : Any> get(type: KClass<T>): T? = serviceRegistry.get(type)

    override fun <T : Any> require(type: KClass<T>): T =
        serviceRegistry.get(type) ?: throw ServiceNotFoundException(type)

    override fun <T : Any> unregister(type: KClass<T>) {
        serviceRegistry.unregister(type)
    }

    override fun unregisterAll(): Int = serviceRegistry.unregisterAll(owner)

    override fun registeredTypes(): Set<KClass<*>> = serviceRegistry.registeredBy(owner)
}
