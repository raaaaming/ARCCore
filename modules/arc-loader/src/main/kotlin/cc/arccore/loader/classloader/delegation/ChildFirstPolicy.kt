package cc.arccore.loader.classloader.delegation

class ChildFirstPolicy : DelegationPolicy {

    override fun resolveClass(context: DelegationContext): DelegationResult {
        val name = context.className

        try {
            val selfClass = context.findClassFromSelf(name)
            return DelegationResult.Found(selfClass)
        } catch (_: ClassNotFoundException) {
        }

        for (dep in context.dependencyLoaders) {
            try {
                val depClass = dep.loadClass(name)
                return DelegationResult.Found(depClass)
            } catch (_: ClassNotFoundException) {
            }
        }

        for (plugin in context.pluginClassLoaders) {
            try {
                val pluginClass = plugin.loadClass(name)
                return DelegationResult.Found(pluginClass)
            } catch (_: ClassNotFoundException) {
            }
        }

        return try {
            val parentClass = context.findClassFromParent(name)
            DelegationResult.Found(parentClass)
        } catch (_: ClassNotFoundException) {
            DelegationResult.NotFound
        }
    }
}
