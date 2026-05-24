package cc.arccore.bootstrap.runtime.lazy

import kotlin.reflect.KClass

/**
 * Provides lazy and eager service wiring against a resolved object graph.
 * The graph is accessed via a generic [Any] reference to avoid compile-time
 * dependency on the generated object graph type.
 *
 * The actual resolution is delegated to [resolver] which is injected by
 * [GeneratedInjectorBridge] after loading the object graph.
 */
class LazyWiringContext(
    val moduleId: String,
    private val resolver: (KClass<*>) -> Any?
) {

    private val lazyEntries: MutableList<Pair<KClass<*>, Lazy<Any?>>> = mutableListOf()
    private val eagerEntries: MutableMap<KClass<*>, Any> = mutableMapOf()

    /**
     * Returns a [Lazy] that resolves [type] from the object graph on first access.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> wireLazy(type: KClass<T>): Lazy<T?> {
        val lazy = lazy { resolver(type) as? T }
        lazyEntries.add(type to lazy)
        return lazy
    }

    /**
     * Immediately resolves [type] from the object graph.
     * @throws IllegalStateException if resolution fails
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> wireEager(type: KClass<T>): T {
        val instance = resolver(type) as? T
            ?: throw IllegalStateException(
                "Failed to eagerly wire '${type.qualifiedName}' for module '$moduleId'"
            )
        eagerEntries[type] = instance
        return instance
    }

    /**
     * Forces initialization of all pending lazy wires and returns the combined [ServiceWiringResult].
     */
    fun flushLazy(): ServiceWiringResult {
        val wired = mutableMapOf<KClass<*>, Any>()
        var failCause: Throwable? = null

        // Flush lazy entries
        for ((type, lazy) in lazyEntries) {
            try {
                val value = lazy.value
                if (value != null) wired[type] = value
            } catch (e: Exception) {
                failCause = e
            }
        }

        // Include eager entries
        wired.putAll(eagerEntries)

        return if (failCause != null) {
            ServiceWiringResult.Failure(moduleId = moduleId, cause = failCause, partiallyWired = wired)
        } else if (wired.isEmpty()) {
            ServiceWiringResult.Empty(moduleId = moduleId)
        } else {
            ServiceWiringResult.Success(
                moduleId = moduleId,
                wiredServices = wired,
                lazyPendingCount = lazyEntries.count { !it.second.isInitialized() },
                eagerWiredCount = eagerEntries.size
            )
        }
    }

    fun pendingLazyCount(): Int = lazyEntries.count { !it.second.isInitialized() }

    fun eagerCount(): Int = eagerEntries.size
}
