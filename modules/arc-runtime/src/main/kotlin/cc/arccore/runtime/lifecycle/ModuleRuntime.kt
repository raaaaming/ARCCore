package cc.arccore.runtime.lifecycle

import cc.arccore.api.ArcAPI
import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.api.module.ModuleContainer
import cc.arccore.api.module.ModuleState
import cc.arccore.loader.loader.DefaultModuleLoader
import cc.arccore.loader.loader.DefaultModuleRegistry
import cc.arccore.loader.loader.ModuleContextFactory
import cc.arccore.loader.loader.ModuleLoader
import cc.arccore.loader.loader.ModuleLoadResult
import cc.arccore.loader.loader.ModuleRegistry
import cc.arccore.loader.loader.SimpleModuleContextFactory
import cc.arccore.loader.metadata.ModuleMetadataReader
import cc.arccore.runtime.context.DefaultModuleContextFactory
import cc.arccore.runtime.context.async.AsyncRuntimeAccess
import cc.arccore.runtime.resource.ResourceTracker
import cc.arccore.runtime.unload.DefaultModuleUnloadManager
import org.bukkit.plugin.Plugin
import java.nio.file.Path
import java.util.logging.Logger

class ModuleRuntime(
    private val arcAPI: ArcAPI,
    private val metadataReader: ModuleMetadataReader,
    private val modulesDirectory: Path,
    private val modulesDataFolder: Path,
    private val parentClassLoader: ClassLoader = ModuleRuntime::class.java.classLoader,
    private val plugin: Plugin? = null,
    private val asyncRuntimeFactory: ((String) -> AsyncRuntimeAccess)? = null
) {
    private val log = Logger.getLogger(ModuleRuntime::class.java.name)
    private val moduleLoader: ModuleLoader
    private val lifecycleManager: ModuleLifecycleManager
    private val orchestrator: LifecycleOrchestrator
    private val eventBus: LifecycleEventBus

    init {
        val orch = LifecycleOrchestrator()
        this.orchestrator = orch

        val unloadManager = DefaultModuleUnloadManager(
            unregisterFromRegistry = {},
            serviceRegistry = arcAPI.serviceRegistry,
            commandRegistry = arcAPI.commandRegistry
        )

        val defaultManager = DefaultModuleLifecycleManager(
            orchestrator = orch,
            unloadManager = unloadManager
        )

        val contextFactory: ModuleContextFactory = if (plugin != null) {
            DefaultModuleContextFactory(
                plugin = plugin,
                bukkit = plugin.server.scheduler,
                lifecycleManager = defaultManager,
                asyncRuntimeFactory = asyncRuntimeFactory
            )
        } else {
            SimpleModuleContextFactory()
        }

        val registry: ModuleRegistry = DefaultModuleRegistry()
        val loader = DefaultModuleLoader(
            arcAPI = arcAPI,
            metadataReader = metadataReader,
            parentClassLoader = parentClassLoader,
            modulesDataFolder = modulesDataFolder,
            registry = registry,
            contextFactory = contextFactory
        )

        this.moduleLoader = loader
        this.lifecycleManager = defaultManager
        this.eventBus = defaultManager.getEventBus()

        val propagator = DependencyFailurePropagator(
            orchestrator = orch,
            lifecycleManager = defaultManager,
            eventBus = this.eventBus,
            containersProvider = { this.moduleLoader.getLoadedContainers() }
        )
        this.eventBus.addObserver(propagator)
    }

    fun loadModules(): List<ModuleLoadResult> {
        log.info("Starting module discovery and loading...")
        val results = moduleLoader.loadAll(modulesDirectory)
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }
        log.info("Module loading complete: $successCount loaded, $failureCount failed")
        return results
    }

    fun enableModules(): List<LifecycleResult> {
        val loaded = getLoadedContainers()
        val enableCandidates = loaded.filter { it.state == ModuleState.LOADED }
        if (enableCandidates.isEmpty()) {
            log.info("No modules to enable")
            return emptyList()
        }
        log.info("Enabling ${enableCandidates.size} module(s)...")
        return lifecycleManager.enableAll(enableCandidates)
    }

    fun disableModules(): List<LifecycleResult> {
        val active = getEnabledContainers()
        if (active.isEmpty()) {
            log.info("No active modules to disable")
            return emptyList()
        }
        log.info("Disabling ${active.size} module(s)...")
        return lifecycleManager.disableAll(active)
    }

    fun unloadModules(): List<LifecycleResult> {
        val loaded = getLoadedContainers()
        if (loaded.isEmpty()) {
            log.info("No modules to unload")
            return emptyList()
        }
        log.info("Unloading ${loaded.size} module(s)...")
        return lifecycleManager.unloadAll(loaded)
    }

    fun startup(): StartupResult {
        val loadResults = loadModules()
        val loadedSuccessfully = loadResults.filterIsInstance<ModuleLoadResult.Success>()

        if (loadedSuccessfully.isEmpty()) {
            log.warning("No modules were loaded successfully")
            return StartupResult(loadResults, emptyList())
        }

        val enableResults = enableModules()
        val enabledCount = enableResults.count { it.isSuccess }
        log.info("Startup complete: ${loadedSuccessfully.size} loaded, $enabledCount enabled")
        return StartupResult(loadResults, enableResults)
    }

    fun shutdown() {
        log.info("Shutting down module runtime...")
        val disableResults = disableModules()
        val unloadResults = unloadModules()
        val failedDisables = disableResults.count { it.isFailure }
        val failedUnloads = unloadResults.count { it.isFailure }
        if (failedDisables > 0 || failedUnloads > 0) {
            log.warning("Shutdown completed with $failedDisables disable(s) and $failedUnloads unload(s) failed")
        } else {
            log.info("Shutdown complete")
        }
    }

    fun enableModule(id: String): LifecycleResult {
        val container = moduleLoader.getContainer(id) ?: return LifecycleResult.NotFound(id)
        return lifecycleManager.enable(container)
    }

    fun disableModule(id: String): LifecycleResult {
        val container = moduleLoader.getContainer(id) ?: return LifecycleResult.NotFound(id)
        return lifecycleManager.disable(container)
    }

    fun unloadModule(id: String): LifecycleResult {
        val container = moduleLoader.getContainer(id) ?: return LifecycleResult.NotFound(id)
        val result = lifecycleManager.unload(container)
        if (result.isSuccess) moduleLoader.unloadModule(id)
        return result
    }

    fun addLifecycleObserver(observer: LifecycleObserver) = eventBus.addObserver(observer)

    fun removeLifecycleObserver(observer: LifecycleObserver) = eventBus.removeObserver(observer)

    fun getModuleLoader(): ModuleLoader = moduleLoader

    fun getLifecycleManager(): ModuleLifecycleManager = lifecycleManager

    fun getRegistry(): ModuleRegistry = moduleLoader.getRegistry()

    fun getResourceTracker(): ResourceTracker =
        (lifecycleManager as? DefaultModuleLifecycleManager)?.getOwnershipTracker()
            ?: error("ResourceTracker unavailable: lifecycle manager is not DefaultModuleLifecycleManager")

    fun getContainers(): Collection<ModuleContainer> = moduleLoader.getLoadedContainers()

    fun getContainer(id: String): ModuleContainer? = moduleLoader.getContainer(id)

    fun getEnabledContainers(): List<ModuleContainer> =
        moduleLoader.getLoadedContainers().filter { it.state == ModuleState.ENABLED }

    fun getLoadedContainers(): List<ModuleContainer> =
        moduleLoader.getLoadedContainers().filter { it.isLoaded() }
}

data class StartupResult(
    val loadResults: List<ModuleLoadResult>,
    val enableResults: List<LifecycleResult>
) {
    val success: Boolean get() = loadResults.any { it.isSuccess }
    val loadedCount: Int get() = loadResults.count { it.isSuccess }
    val enabledCount: Int get() = enableResults.count { it.isSuccess }
    val failedLoadCount: Int get() = loadResults.count { it.isFailure }
    val failedEnableCount: Int get() = enableResults.count { it.isFailure }
}
