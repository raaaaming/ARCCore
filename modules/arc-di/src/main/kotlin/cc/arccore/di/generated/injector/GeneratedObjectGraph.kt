package cc.arccore.di.generated.injector

import cc.arccore.api.di.DependencyResolver
import cc.arccore.api.di.Scope
import cc.arccore.api.di.exception.ObjectGraphException
import cc.arccore.api.di.generated.GeneratedInjector
import cc.arccore.api.di.generated.GeneratedProvider
import cc.arccore.api.di.generated.InjectionContext
import cc.arccore.di.generated.injector.provider.GeneratedModuleProvider
import cc.arccore.di.generated.injector.provider.GeneratedSingletonProvider
import cc.arccore.di.generated.injector.provider.GeneratedTransientProvider
import cc.arccore.di.generated.injector.singleton.SingletonCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Compile-time generated object graph for a single module load cycle.
 *
 * This is the central runtime component for the Generated Injection system.
 * It wires together generated factories, scope providers, and the parent
 * DI container into a single resolution chain:
 *
 * ```
 * graph.resolve(BalanceCommand::class)
 *   → GeneratedBalanceCommandFactory.create(this)
 *     → graph.resolve(EconomyService::class)       // peer @ArcComponent
 *       → GeneratedEconomyServiceFactory.create()
 *         → graph.resolve(DatabaseService::class)  // falls through to parent if external
 *           → parent.resolve(DatabaseService, moduleId) → ServiceRegistry
 * ```
 *
 * WHY this eliminates reflection overhead at every resolution call:
 * - No `Class.forName`, no `getConstructors()`, no `newInstance()`.
 * - No `KClass.constructors`, no `KParameter` iteration.
 * - `ConcurrentHashMap.computeIfAbsent` for singleton caching is O(1) after
 *   the first call; reflection-based resolution re-scans parameters each time.
 * - Startup: the graph is built by iterating a pre-loaded list; no classpath scan.
 *
 * WHY the parent delegate for unregistered types:
 * - Not all injectable types will be @ArcComponent (e.g., external services,
 *   manually bound instances). Delegating to the parent makes the generated
 *   path transparent to the calling code: callers always use the same
 *   [DependencyResolver] interface regardless of which path resolves the type.
 *
 * WHY per-module isolation:
 * - Paper plugins with their own ClassLoaders share a JVM but must not share
 *   module-scoped state. Each module load gets a fresh graph. Singleton-scope
 *   instances live in this graph's [SingletonCache]; module-scope instances
 *   live in [moduleInstances]. Both are fully cleaned up on [cleanup].
 * - This makes hot-reload safe: the old graph is cleaned up, the new load
 *   creates a new graph, and there is no shared mutable state between the two.
 */
class GeneratedObjectGraph(
    override val moduleId: String,
    private val parent: DependencyResolver
) : InjectionContext {

    private val injectors = ConcurrentHashMap<KClass<*>, GeneratedInjector<*>>()
    private val providers = ConcurrentHashMap<KClass<*>, GeneratedProvider<*>>()
    private val singletonCache = SingletonCache()
    private val moduleInstances = ConcurrentHashMap<KClass<*>, Any>()

    fun register(injector: GeneratedInjector<*>) {
        injectors[injector.targetClass] = injector
        providers[injector.targetClass] = when (injector.scope) {
            Scope.Singleton -> GeneratedSingletonProvider(injector, singletonCache)
            Scope.Module -> GeneratedModuleProvider(injector, moduleInstances)
            Scope.Transient -> GeneratedTransientProvider(injector)
        }
    }

    fun isRegistered(type: KClass<*>): Boolean = providers.containsKey(type)

    fun registeredCount(): Int = injectors.size

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolve(type: KClass<T>): T {
        val provider = providers[type] as? GeneratedProvider<T>
        if (provider != null) return provider.get(this)

        // Unregistered type: delegate to parent container.
        // The parent will NOT call back into this graph for unregistered types,
        // so there is no infinite recursion.
        return if (parent.canResolve(type, moduleId)) {
            parent.resolve(type, moduleId)
        } else {
            throw ObjectGraphException(
                moduleId,
                "No generated injector or registered binding for '${type.qualifiedName}'"
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolveOrNull(type: KClass<T>): T? {
        val provider = providers[type] as? GeneratedProvider<T>
        if (provider != null) return provider.get(this)
        return if (parent.canResolve(type, moduleId)) parent.resolve(type, moduleId) else null
    }

    /**
     * Releases all cached instances and calls [GeneratedInjector.cleanup] on each.
     *
     * Called by [cc.arccore.di.generated.injector.lifecycle.GeneratedDILifecycleManager]
     * on module UNLOADED / FAILED / DEPENDENCY_FAILED events. After this call the
     * graph is empty and must not be used again — a new one is created on the next
     * module load.
     */
    fun cleanup() {
        for ((type, instance) in moduleInstances) {
            @Suppress("UNCHECKED_CAST")
            (injectors[type] as? GeneratedInjector<Any>)?.cleanup(instance)
        }
        moduleInstances.clear()

        val singletons = singletonCache.clearWithTypes()
        for ((type, instance) in singletons) {
            @Suppress("UNCHECKED_CAST")
            (injectors[type] as? GeneratedInjector<Any>)?.cleanup(instance)
        }

        injectors.clear()
        providers.clear()
    }
}
