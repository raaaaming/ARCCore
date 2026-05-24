package cc.arccore.loader.loader

import cc.arccore.api.module.ModuleContainer
import java.nio.file.Path

interface ModuleLoader {

    fun loadAll(modulesDirectory: Path): List<ModuleLoadResult>

    fun loadModule(jarPath: Path): ModuleLoadResult

    fun unloadModule(id: String): Boolean

    fun getRegistry(): ModuleRegistry

    fun getLoadedContainers(): Collection<ModuleContainer>

    fun getContainer(id: String): ModuleContainer?
}
