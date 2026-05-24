package cc.arccore.runtime.lifecycle

import cc.arccore.api.module.ModuleContainer

fun interface ModuleCleanupHook {

    fun cleanup(container: ModuleContainer)
}

class ModuleCleanupRegistry {

    private val hooks = mutableListOf<ModuleCleanupHook>()

    fun register(hook: ModuleCleanupHook) {
        hooks.add(hook)
    }

    fun registerAll(hooks: Collection<ModuleCleanupHook>) {
        this.hooks.addAll(hooks)
    }

    fun runCleanup(container: ModuleContainer) {
        for (hook in hooks) {
            try {
                hook.cleanup(container)
            } catch (_: Exception) {
            }
        }
    }

    fun clear() {
        hooks.clear()
    }

    fun getHooks(): List<ModuleCleanupHook> = hooks.toList()
}
