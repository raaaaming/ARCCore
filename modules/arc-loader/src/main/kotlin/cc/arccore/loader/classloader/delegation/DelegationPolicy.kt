package cc.arccore.loader.classloader.delegation

import cc.arccore.loader.ModuleClassLoader

data class DelegationContext(
    val className: String,
    val selfLoader: ModuleClassLoader,
    val parentLoader: ClassLoader,
    val dependencyLoaders: List<ModuleClassLoader>,
    val pluginClassLoaders: List<ClassLoader>,
    val findClassFromSelf: (String) -> Class<*>,
    val findClassFromParent: (String) -> Class<*>
)

sealed class DelegationResult {
    data class Found(val clazz: Class<*>) : DelegationResult()
    data object NotFound : DelegationResult()
}

fun interface DelegationPolicy {

    fun resolveClass(context: DelegationContext): DelegationResult

    companion object {
        fun parentFirst(): DelegationPolicy = ParentFirstPolicy()
        fun childFirst(): DelegationPolicy = ChildFirstPolicy()
        fun hybrid(sharedFilter: (String) -> Boolean): DelegationPolicy = HybridPolicy(sharedFilter)
    }
}
