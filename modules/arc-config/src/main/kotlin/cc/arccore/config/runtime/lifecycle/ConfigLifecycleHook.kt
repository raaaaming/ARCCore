package cc.arccore.config.runtime.lifecycle

interface ConfigLifecycleHook {
    fun onConfigLoaded(moduleId: String, path: String, clazz: String) {}
    fun onConfigReloaded(moduleId: String, path: String, generation: Long) {}
    fun onConfigUnloaded(moduleId: String, path: String) {}
    fun onShutdown() {}
}
