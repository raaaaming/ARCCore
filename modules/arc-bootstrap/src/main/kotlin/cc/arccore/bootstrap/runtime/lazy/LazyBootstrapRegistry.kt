package cc.arccore.bootstrap.runtime.lazy

import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry for [LazyWiringContext] instances per module.
 * Allows service wiring contexts to be looked up after bootstrap completes.
 */
class LazyBootstrapRegistry {

    private val contexts: ConcurrentHashMap<String, LazyWiringContext> = ConcurrentHashMap()

    fun register(moduleId: String, context: LazyWiringContext) {
        contexts[moduleId] = context
    }

    fun getContext(moduleId: String): LazyWiringContext? = contexts[moduleId]

    fun remove(moduleId: String) {
        contexts.remove(moduleId)
    }

    fun flushAll(): Map<String, ServiceWiringResult> =
        contexts.mapValues { (_, ctx) -> ctx.flushLazy() }

    fun flushModule(moduleId: String): ServiceWiringResult? =
        contexts[moduleId]?.flushLazy()

    fun registeredModuleIds(): Set<String> = contexts.keys.toSet()

    fun clear() = contexts.clear()
}
