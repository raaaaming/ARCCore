package cc.arccore.api.module

import cc.arccore.api.module.reload.ReloadResult

interface ModuleManager {

    fun register(module: ArcModuleAPI)

    fun unregister(id: String)

    fun getModule(id: String): ArcModuleAPI?

    fun getModules(): Collection<ArcModuleAPI>

    fun getState(id: String): ModuleState?

    fun isLoaded(id: String): Boolean

    fun reload(id: String): ReloadResult
}
