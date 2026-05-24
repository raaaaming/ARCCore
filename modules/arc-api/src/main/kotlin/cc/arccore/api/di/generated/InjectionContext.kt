package cc.arccore.api.di.generated

import kotlin.reflect.KClass

/**
 * Resolution context passed to generated injector factories.
 *
 * Generated factories call [resolve] to obtain dependencies instead of using
 * reflection. The implementation ([GeneratedObjectGraph]) handles scope caching
 * and delegates unregistered types to the parent DI container.
 *
 * WHY this exists separately from [cc.arccore.api.di.ResolutionContext]:
 * - Generated factories must not carry a moduleId parameter on every call;
 *   the graph is already scoped to a module.
 * - Decoupling lets the graph implement both resolution interfaces without
 *   creating API leakage between the reflection-based and generated paths.
 */
interface InjectionContext {

    val moduleId: String

    fun <T : Any> resolve(type: KClass<T>): T

    fun <T : Any> resolveOrNull(type: KClass<T>): T? = try {
        resolve(type)
    } catch (_: Exception) {
        null
    }
}
