package cc.arccore.di.generated.injector.provider

import cc.arccore.api.di.generated.GeneratedInjector
import cc.arccore.api.di.generated.GeneratedProvider
import cc.arccore.api.di.generated.InjectionContext

/**
 * Provider that creates a new instance on every [get] call.
 *
 * Default scope for @ArcComponent classes without an explicit scope annotation.
 * Commands and listeners are typically transient — they are stateless handlers
 * registered once with Bukkit, not objects that accumulate state between calls.
 */
class GeneratedTransientProvider<T : Any>(
    private val injector: GeneratedInjector<T>
) : GeneratedProvider<T> {

    override fun get(context: InjectionContext): T = injector.create(context)
}
