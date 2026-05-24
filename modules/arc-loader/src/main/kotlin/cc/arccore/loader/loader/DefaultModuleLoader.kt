package cc.arccore.loader.loader

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ModuleContainer
import cc.arccore.loader.metadata.ModuleMetadataReader
import java.nio.file.Path
import java.util.logging.Logger

class DefaultModuleLoader(
    private val arcAPI: ArcAPI,
    private val metadataReader: ModuleMetadataReader,
    private val parentClassLoader: ClassLoader = DefaultModuleLoader::class.java.classLoader,
    private val modulesDataFolder: Path,
    private val registry: ModuleRegistry = DefaultModuleRegistry(),
    private val contextFactory: ModuleContextFactory = SimpleModuleContextFactory()
) : ModuleLoader {

    private val log = Logger.getLogger(DefaultModuleLoader::class.java.name)
    private val pipeline = ModuleLoadPipeline(
        arcAPI = arcAPI,
        metadataReader = metadataReader,
        parentClassLoader = parentClassLoader,
        modulesDataFolder = modulesDataFolder,
        registry = registry,
        contextFactory = contextFactory
    )

    override fun loadAll(modulesDirectory: Path): List<ModuleLoadResult> {
        val results = pipeline.loadAll(modulesDirectory)
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }
        log.info("Module loading complete: $successCount loaded, $failureCount failed")
        return results
    }

    override fun loadModule(jarPath: Path): ModuleLoadResult {
        return pipeline.loadSingle(jarPath)
    }

    override fun unloadModule(id: String): Boolean {
        val container = registry.get(id) ?: return false
        registry.unregister(id)
        log.info("Unregistered module '$id' from registry")
        return true
    }

    override fun getRegistry(): ModuleRegistry = registry

    override fun getLoadedContainers(): Collection<ModuleContainer> = registry.getAll()

    override fun getContainer(id: String): ModuleContainer? = registry.get(id)
}
