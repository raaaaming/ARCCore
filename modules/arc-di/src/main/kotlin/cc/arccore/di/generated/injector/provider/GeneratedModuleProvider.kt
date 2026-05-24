package cc.arccore.di.generated.injector.provider

import cc.arccore.api.di.generated.GeneratedInjector
import cc.arccore.api.di.generated.GeneratedProvider
import cc.arccore.api.di.generated.InjectionContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Provider that creates one instance per module load cycle and caches it
 * in the graph's module-scoped store.
 *
 * The instance is destroyed when [cc.arccore.di.generated.injector.GeneratedObjectGraph.cleanup]
 * is called on module unload, ensuring reload-safe isolation.
 */
class GeneratedModuleProvider<T : Any>(
    private val injector: GeneratedInjector<T>,
    private val moduleInstances: ConcurrentHashMap<KClass<*>, Any>
) : GeneratedProvider<T> {

    @Suppress("UNCHECKED_CAST")
    override fun get(context: InjectionContext): T =
        moduleInstances.computeIfAbsent(injector.targetClass) { injector.create(context) } as T
}
