package cc.arccore.di.generated.injector.provider

import cc.arccore.api.di.generated.GeneratedInjector
import cc.arccore.api.di.generated.GeneratedProvider
import cc.arccore.api.di.generated.InjectionContext
import cc.arccore.di.generated.injector.singleton.SingletonCache

/**
 * Provider that creates an instance once and caches it for the JVM lifetime.
 *
 * Suitable for stateless, heavy-to-construct services (database pools, HTTP clients).
 * The singleton is shared across ALL module reloads — use [GeneratedModuleProvider]
 * if you need per-load isolation.
 */
class GeneratedSingletonProvider<T : Any>(
    private val injector: GeneratedInjector<T>,
    private val cache: SingletonCache
) : GeneratedProvider<T> {

    override fun get(context: InjectionContext): T =
        cache.getOrCreate(injector.targetClass) { injector.create(context) }
}
