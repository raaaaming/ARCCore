package cc.arccore.zerodowntime.runtime.integration

import cc.arccore.zerodowntime.runtime.ZeroDowntimeReloadRuntime
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeReloadResult

class RuntimeBridge(
    private val zdtRuntime: ZeroDowntimeReloadRuntime,
    private val fallbackReloader: FallbackReloader? = null
) {
    fun interface FallbackReloader {
        fun reload(moduleId: String): Boolean
    }

    fun reload(moduleId: String, forceZeroDowntime: Boolean = false): Any {
        if (forceZeroDowntime || shouldUseZeroDowntime(moduleId)) {
            return zdtRuntime.reload(moduleId)
        }
        return fallbackReloader?.reload(moduleId) ?: zdtRuntime.reload(moduleId)
    }

    fun reloadAll(moduleIds: List<String>): Map<String, ZeroDowntimeReloadResult> {
        return zdtRuntime.reloadAll(moduleIds)
    }

    fun shouldUseZeroDowntime(moduleId: String): Boolean = true

    fun getZdtRuntime(): ZeroDowntimeReloadRuntime = zdtRuntime
}
