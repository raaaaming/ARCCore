package cc.arccore.runtime.reload

import cc.arccore.api.module.reload.ReloadResult

interface HotReloadManager {
    fun reload(moduleId: String): ReloadResult
    fun reloadAll(moduleIds: List<String>): Map<String, ReloadResult>
    fun isReloading(): Boolean
}
