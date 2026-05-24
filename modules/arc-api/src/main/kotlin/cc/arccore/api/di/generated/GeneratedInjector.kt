package cc.arccore.api.di.generated

import cc.arccore.api.di.Scope
import kotlin.reflect.KClass

/**
 * Compile-time generated factory for a single injectable type.
 *
 * KSP generates one implementation per @ArcComponent class. The implementation
 * calls the target class's constructor directly — no reflection at all:
 *
 * ```
 * class GeneratedBalanceCommandFactory : GeneratedInjector<BalanceCommand> {
 *     override val targetClass = BalanceCommand::class
 *     override val scope = Scope.Transient
 *     override fun create(context: InjectionContext) =
 *         BalanceCommand(context.resolve(EconomyService::class))
 * }
 * ```
 *
 * WHY generated over reflection:
 * - Direct constructor call is JIT-inlineable; `constructor.newInstance()` is not.
 * - No Method/Parameter/Annotation objects allocated per resolve call.
 * - Circular dependency and missing dependency errors surface at compile time,
 *   not first request on a live server.
 * - AOT-compatible — GraalVM native image can see the call graph statically.
 *
 * [metadataVersion] is checked at load time to catch stale generated artifacts
 * (e.g., incremental build that regenerated only some factories).
 */
interface GeneratedInjector<T : Any> {

    val targetClass: KClass<T>

    val scope: Scope

    val metadataVersion: Int get() = 1

    fun create(context: InjectionContext): T

    fun cleanup(instance: T) {}
}
