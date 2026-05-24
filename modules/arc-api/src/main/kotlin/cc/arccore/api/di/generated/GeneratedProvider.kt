package cc.arccore.api.di.generated

/**
 * Scope-aware wrapper around a [GeneratedInjector].
 *
 * Implementations handle lifetime policy:
 * - [cc.arccore.di.generated.injector.provider.GeneratedSingletonProvider] — one instance per JVM
 * - [cc.arccore.di.generated.injector.provider.GeneratedModuleProvider] — one instance per module load
 * - [cc.arccore.di.generated.injector.provider.GeneratedTransientProvider] — new instance every call
 *
 * Kept in arc-api so that future extension points (lazy, pooled, weak) can be
 * contributed from external modules without a dependency on arc-di internals.
 */
interface GeneratedProvider<T : Any> {
    fun get(context: InjectionContext): T
}
