package cc.arccore.loader.classloader.delegation

import cc.arccore.loader.ModuleClassLoader

class ParentFirstPolicy : DelegationPolicy {

    override fun resolveClass(context: DelegationContext): DelegationResult {
        val name = context.className

        try {
            val parentClass = context.findClassFromParent(name)
            return DelegationResult.Found(parentClass)
        } catch (_: ClassNotFoundException) {
        }

        for (dep in context.dependencyLoaders) {
            try {
                val depClass = dep.loadClass(name)
                return DelegationResult.Found(depClass)
            } catch (_: ClassNotFoundException) {
            }
        }

        return try {
            val selfClass = context.findClassFromSelf(name)
            DelegationResult.Found(selfClass)
        } catch (_: ClassNotFoundException) {
            DelegationResult.NotFound
        }
    }
}
