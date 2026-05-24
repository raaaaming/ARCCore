package cc.arccore.config.runtime.integration

import cc.arccore.config.runtime.reload.ReloadResult

interface DiagnosticsConfigBridgePort {
    fun onConfigLoaded(moduleId: String, path: String, clazz: String, durationNanos: Long) {}
    fun onConfigReloaded(moduleId: String, path: String, result: ReloadResult) {}
    fun onConfigUnloaded(moduleId: String, path: String) {}
    fun onShutdown() {}
}
