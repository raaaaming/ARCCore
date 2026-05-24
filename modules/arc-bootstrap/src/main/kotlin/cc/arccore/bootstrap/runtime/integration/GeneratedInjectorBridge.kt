package cc.arccore.bootstrap.runtime.integration

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.BootstrapContextKey
import cc.arccore.bootstrap.runtime.lazy.LazyBootstrapRegistry
import cc.arccore.bootstrap.runtime.lazy.LazyWiringContext

/**
 * Bridge to the KSP-generated DI object graph.
 *
 * Reads `META-INF/arc/injectors.list` from the module's ClassLoader.
 * If present, loads injectors via `cc.arccore.di.generated.InjectorLoader`
 * (or its equivalent) and stores the resolved object graph into BootstrapContext.
 */
class GeneratedInjectorBridge(
    private val lazyRegistry: LazyBootstrapRegistry
) {

    companion object {
        private const val INJECTORS_LIST_PATH = "META-INF/arc/injectors.list"
        private const val INJECTOR_LOADER_CLASS = "cc.arccore.di.generated.InjectorLoader"
    }

    sealed class InjectorLoadResult {
        data class Loaded(val objectGraph: Any, val injectorCount: Int) : InjectorLoadResult()
        data class NotFound(val reason: String) : InjectorLoadResult()
        data class Failed(val cause: Throwable, val message: String) : InjectorLoadResult()

        val isLoaded: Boolean get() = this is Loaded
    }

    /**
     * Loads the generated object graph and stores it in the BootstrapContext.
     * Also registers a [LazyWiringContext] for deferred service resolution.
     */
    fun loadAndWire(context: BootstrapContext): InjectorLoadResult {
        val classLoader = context.classLoader

        // 1. Check injectors.list
        val injectorListStream = classLoader.getResourceAsStream(INJECTORS_LIST_PATH)
            ?: return InjectorLoadResult.NotFound(
                "injectors.list not found at '$INJECTORS_LIST_PATH' for '${context.moduleId}'"
            )

        val injectorClassNames = injectorListStream.use { stream ->
            stream.bufferedReader(Charsets.UTF_8)
                .readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        if (injectorClassNames.isEmpty()) {
            return InjectorLoadResult.NotFound(
                "injectors.list is empty for '${context.moduleId}'"
            )
        }

        // 2. Load InjectorLoader via reflection
        return try {
            val loaderClass = classLoader.loadClass(INJECTOR_LOADER_CLASS)
            val loadMethod = loaderClass.getDeclaredMethod("loadAll", ClassLoader::class.java, List::class.java)
            val objectGraph = loadMethod.invoke(null, classLoader, injectorClassNames)
                ?: return InjectorLoadResult.Failed(
                    cause = NullPointerException("InjectorLoader.loadAll returned null"),
                    message = "InjectorLoader produced null graph for '${context.moduleId}'"
                )

            // 3. Store object graph in context
            context.put(BootstrapContextKey.GENERATED_OBJECT_GRAPH, objectGraph)

            // 4. Register lazy wiring context
            val wiringContext = buildWiringContext(context.moduleId, objectGraph, loaderClass)
            lazyRegistry.register(context.moduleId, wiringContext)

            InjectorLoadResult.Loaded(objectGraph = objectGraph, injectorCount = injectorClassNames.size)
        } catch (e: ClassNotFoundException) {
            InjectorLoadResult.NotFound(
                "InjectorLoader class '$INJECTOR_LOADER_CLASS' not found — arc-di may not be on classpath"
            )
        } catch (e: Exception) {
            InjectorLoadResult.Failed(
                cause = e,
                message = "Failed to load generated injectors for '${context.moduleId}': ${e.message}"
            )
        }
    }

    private fun buildWiringContext(
        moduleId: String,
        objectGraph: Any,
        loaderClass: Class<*>
    ): LazyWiringContext {
        // Attempt to find a resolveAs(KClass) method on the object graph
        val graphClass = objectGraph::class.java
        val resolveMethod = try {
            graphClass.getDeclaredMethod("resolveAs", Class::class.java)
        } catch (_: NoSuchMethodException) {
            null
        }

        val resolver: (kotlin.reflect.KClass<*>) -> Any? = { kClass ->
            try {
                resolveMethod?.invoke(objectGraph, kClass.java)
            } catch (_: Exception) {
                null
            }
        }

        return LazyWiringContext(moduleId = moduleId, resolver = resolver)
    }
}
