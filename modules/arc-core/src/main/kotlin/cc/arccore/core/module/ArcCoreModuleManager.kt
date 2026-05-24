package cc.arccore.core.module

import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ModuleManager
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.reload.ReloadResult
import cc.arccore.runtime.lifecycle.ModuleRuntime
import cc.arccore.runtime.reload.HotReloadManager
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ArcCoreModuleManager(
    private val runtime: ModuleRuntime
) : ModuleManager {

    private val jarPaths = ConcurrentHashMap<String, Path>()
    private val programmaticModules = ConcurrentHashMap<String, ArcModuleAPI>()
    private var hotReloadManager: HotReloadManager? = null

    fun setHotReloadManager(manager: HotReloadManager) {
        hotReloadManager = manager
    }

    fun trackJarPath(moduleId: String, jarPath: Path) {
        jarPaths[moduleId] = jarPath
    }

    fun getJarPath(id: String): Path? = jarPaths[id]

    override fun register(module: ArcModuleAPI) {
        val id = module.id
        if (programmaticModules.containsKey(id) || runtime.getContainer(id) != null) {
            throw IllegalStateException("Module '$id' is already registered")
        }
        programmaticModules[id] = module
    }

    override fun unregister(id: String) {
        programmaticModules.remove(id)
        if (runtime.getContainer(id) != null) {
            runtime.disableModule(id)
            runtime.unloadModule(id)
        }
        jarPaths.remove(id)
    }

    override fun getModule(id: String): ArcModuleAPI? {
        return programmaticModules[id] ?: runtime.getContainer(id)?.module
    }

    override fun getModules(): Collection<ArcModuleAPI> {
        val runtimeModules = runtime.getContainers().map { it.module }
        return programmaticModules.values + runtimeModules
    }

    override fun getState(id: String): ModuleState? {
        val prog = programmaticModules[id]
        if (prog != null) return ModuleState.ENABLED
        return runtime.getContainer(id)?.state
    }

    override fun isLoaded(id: String): Boolean {
        if (programmaticModules.containsKey(id)) return true
        return runtime.getContainer(id)?.isLoaded() ?: false
    }

    override fun reload(id: String): ReloadResult {
        val hrm = hotReloadManager
        if (hrm != null) {
            return hrm.reload(id)
        }
        // fallback: HotReloadManager가 설정되지 않은 경우 naive reload
        val jarPath = jarPaths[id]
            ?: return ReloadResult.Failure(
                id, "LEGACY",
                UnsupportedOperationException("HotReloadManager not set and jar path unknown for '$id'")
            )
        return try {
            runtime.disableModule(id)
            runtime.unloadModule(id)
            val result = runtime.getModuleLoader().loadModule(jarPath)
            if (result.isSuccess) {
                runtime.enableModule(id)
                ReloadResult.Success(id, emptyList(), 0L)
            } else {
                ReloadResult.Failure(
                    id, "LEGACY",
                    result.exceptionOrNull() ?: RuntimeException("Load failed for '$id'")
                )
            }
        } catch (e: Exception) {
            ReloadResult.Failure(id, "LEGACY", e)
        }
    }
}
