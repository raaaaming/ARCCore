package cc.arccore.config.runtime.integration

import cc.arccore.config.runtime.reload.ReloadResult

object NoopDiagnosticsConfigBridgePort : DiagnosticsConfigBridgePort {
    override fun onConfigLoaded(moduleId: String, path: String, clazz: String, durationNanos: Long) {}
    override fun onConfigReloaded(moduleId: String, path: String, result: ReloadResult) {}
    override fun onConfigUnloaded(moduleId: String, path: String) {}
    override fun onShutdown() {}
}
