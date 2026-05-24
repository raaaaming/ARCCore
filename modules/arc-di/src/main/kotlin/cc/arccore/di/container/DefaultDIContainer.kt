package cc.arccore.di.container

import cc.arccore.api.di.*
import cc.arccore.api.di.exception.*
import cc.arccore.api.service.ServiceRegistry
import cc.arccore.di.generated.injector.GeneratedObjectGraph
import cc.arccore.di.generated.injector.InjectorLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class DefaultDIContainer(
    private val serviceRegistry: ServiceRegistry? = null,
    private val validator: CircularDependencyValidator = CircularDependencyValidator(),
    private val injector: ConstructorInjector = ConstructorInjector()
) : DIContainer {

    private val singletonProviders = ConcurrentHashMap<KClass<*>, InstanceProvider<*>>()
    private val singletonInstances = ConcurrentHashMap<KClass<*>, Any>()
    private val moduleProviders = ConcurrentHashMap<String, ConcurrentHashMap<KClass<*>, InstanceProvider<*>>>()
    private val moduleInstances = ConcurrentHashMap<String, ConcurrentHashMap<KClass<*>, Any>>()

    // Generated object graph per module — populated by GeneratedDILifecycleManager on ENABLED.
    private val generatedGraphs = ConcurrentHashMap<String, GeneratedObjectGraph>()

    /**
     * Loads generated injectors from the module's ClassLoader and registers
     * a [GeneratedObjectGraph] for this module. Called once per module load cycle.
     *
     * No-op if no `META-INF/arc/generated/injectors.list` resource is found,
     * which is the case for modules that don't use @ArcComponent.
     */
    fun initializeGeneratedGraph(moduleId: String, classLoader: ClassLoader) {
        val loaded = InjectorLoader.load(classLoader)
        if (loaded.isEmpty()) return
        val graph = GeneratedObjectGraph(moduleId, this)
        loaded.forEach { graph.register(it) }
        generatedGraphs[moduleId] = graph
    }

    /**
     * Tears down the generated object graph for [moduleId], invoking cleanup
     * callbacks on all cached instances.
     */
    fun cleanupGeneratedGraph(moduleId: String) {
        generatedGraphs.remove(moduleId)?.cleanup()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolve(type: KClass<T>, moduleId: String): T {
        validator.enter(type)
        try {
            // Generated path — O(1) map lookup, direct constructor call, no reflection.
            // The graph delegates back here (via DependencyResolver) for types it does
            // not own, so there is no infinite recursion for external dependencies.
            generatedGraphs[moduleId]?.let { graph ->
                if (graph.isRegistered(type)) return graph.resolve(type)
            }

            val context = InnerResolutionContext(moduleId)

            moduleInstances[moduleId]?.get(type)?.let { return it as T }

            moduleProviders[moduleId]?.get(type)?.let { provider ->
                return moduleInstances.computeIfAbsent(moduleId) { ConcurrentHashMap() }
                    .computeIfAbsent(type) { (provider as InstanceProvider<T>).provide(context) } as T
            }

            singletonInstances[type]?.let { return it as T }

            singletonProviders[type]?.let { provider ->
                @Suppress("UNCHECKED_CAST")
                return singletonInstances.computeIfAbsent(type) {
                    (provider as InstanceProvider<T>).provide(context)
                } as T
            }

            serviceRegistry?.get(type)?.let { return it }

            // Reflection fallback — used when no generated injector was produced
            // (e.g., the module was compiled without arc-ksp, or a class was added
            // after the last KSP run without a rebuild).
            if (!injector.canInstantiate(type)) {
                throw MissingDependencyException(
                    type.qualifiedName ?: type.simpleName ?: "Unknown",
                    "DIContainer"
                )
            }
            return injector.instantiate(type, context)
        } finally {
            validator.exit(type)
        }
    }

    override fun <T : Any> canResolve(type: KClass<T>, moduleId: String): Boolean {
        return generatedGraphs[moduleId]?.isRegistered(type) == true ||
            moduleInstances[moduleId]?.containsKey(type) == true ||
            moduleProviders[moduleId]?.containsKey(type) == true ||
            singletonInstances.containsKey(type) ||
            singletonProviders.containsKey(type) ||
            serviceRegistry?.isRegistered(type) == true ||
            injector.canInstantiate(type)
    }

    override fun <T : Any> bindSingleton(type: KClass<T>, provider: InstanceProvider<T>) {
        singletonProviders[type] = provider
    }

    override fun <T : Any> bindModule(moduleId: String, type: KClass<T>, provider: InstanceProvider<T>) {
        moduleProviders.computeIfAbsent(moduleId) { ConcurrentHashMap() }[type] = provider
    }

    override fun <T : Any> bindInstance(type: KClass<T>, instance: T) {
        singletonInstances[type] = instance
    }

    override fun <T : Any> bindModuleInstance(moduleId: String, type: KClass<T>, instance: T) {
        moduleInstances.computeIfAbsent(moduleId) { ConcurrentHashMap() }[type] = instance
    }

    override fun clearModule(moduleId: String) {
        cleanupGeneratedGraph(moduleId)
        moduleInstances.remove(moduleId)?.clear()
        moduleProviders.remove(moduleId)?.clear()
    }

    override fun <T : Any> isBound(type: KClass<T>): Boolean =
        singletonInstances.containsKey(type) || singletonProviders.containsKey(type)

    override fun <T : Any> isModuleBound(moduleId: String, type: KClass<T>): Boolean =
        moduleInstances[moduleId]?.containsKey(type) == true ||
            moduleProviders[moduleId]?.containsKey(type) == true

    private inner class InnerResolutionContext(override val moduleId: String) : ResolutionContext {
        override fun <T : Any> resolve(type: KClass<T>): T = this@DefaultDIContainer.resolve(type, moduleId)
    }
}
