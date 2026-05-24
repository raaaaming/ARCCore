package cc.arccore.core

import cc.arccore.api.ArcAPI
import cc.arccore.api.ArcPlugin
import cc.arccore.core.command.ArcDiagnosticsCommand
import cc.arccore.core.command.ArcLeaksCommand
import cc.arccore.core.command.ArcReloadCommand
import cc.arccore.core.module.ArcCoreModuleManager
import cc.arccore.coroutine.CoroutineRuntimeFactory
import cc.arccore.diagnostics.DefaultRuntimeDiagnosticsManager
import cc.arccore.diagnostics.leak.DefaultLeakDetectionManager
import cc.arccore.diagnostics.leak.LeakDetectionManager
import cc.arccore.diagnostics.leak.model.LeakReport
import cc.arccore.diagnostics.leak.model.LeakSeverity
import cc.arccore.diagnostics.leak.model.LeakType
import cc.arccore.loader.loader.ModuleLoadResult
import cc.arccore.loader.metadata.JsonModuleMetadataReader
import cc.arccore.runtime.lifecycle.ModuleRuntime
import cc.arccore.runtime.reload.DefaultHotReloadManager
import cc.arccore.runtime.reload.HotReloadManager
import cc.arccore.runtime.annotation.generated.GeneratedBootstrapPipeline
import cc.arccore.runtime.resource.ResourceTracker
import cc.arccore.runtime.resource.integration.ResourceLeakBridge
import org.bukkit.command.TabCompleter

internal class ArcCorePluginDelegate(
    private val plugin: ArcCorePlugin
) : ArcPlugin {

    override val pluginId: String get() = "ARCCore"
    override val pluginVersion: String get() = plugin.pluginMeta.version

    override lateinit var api: ArcAPI
        private set

    private lateinit var runtime: ModuleRuntime
    private lateinit var moduleManager: ArcCoreModuleManager
    private lateinit var hotReloadManager: HotReloadManager
    private lateinit var diagnosticsManager: DefaultRuntimeDiagnosticsManager
    private lateinit var leakDetectionManager: LeakDetectionManager
    private lateinit var resourceTracker: ResourceTracker

    override fun onLoad() {
        val coreAPI = ArcCoreAPI(this)
        api = coreAPI

        val dataDir = plugin.dataFolder.toPath()
        val modulesDir = dataDir.resolve("modules")
        val modulesDataDir = dataDir.resolve("modules-data")
        val metadataReader = JsonModuleMetadataReader()

        val coroutineRuntimeFactory = CoroutineRuntimeFactory(plugin)
        runtime = ModuleRuntime(
            arcAPI = coreAPI,
            metadataReader = metadataReader,
            modulesDirectory = modulesDir,
            modulesDataFolder = modulesDataDir,
            parentClassLoader = ArcCorePlugin::class.java.classLoader,
            plugin = plugin,
            asyncRuntimeFactory = { moduleId -> coroutineRuntimeFactory.create(moduleId) }
        )

        moduleManager = ArcCoreModuleManager(runtime)
        coreAPI.moduleManager = moduleManager

        hotReloadManager = DefaultHotReloadManager(
            runtime = runtime,
            jarPathProvider = { id -> moduleManager.getJarPath(id) }
        )
        moduleManager.setHotReloadManager(hotReloadManager)

        diagnosticsManager = DefaultRuntimeDiagnosticsManager(runtime)
        runtime.addLifecycleObserver(diagnosticsManager.lifecycleObserver)

        leakDetectionManager = DefaultLeakDetectionManager(runtime)
        runtime.addLifecycleObserver(leakDetectionManager.lifecycleObserver)

        runtime.addLifecycleObserver(GeneratedBootstrapPipeline(plugin))

        resourceTracker = runtime.getResourceTracker()
        val leakBridge = ResourceLeakBridge(
            onOrphanDetected = { desc ->
                leakDetectionManager.recordLeak(LeakReport(
                    moduleId = desc.moduleId,
                    type = when (desc.type) {
                        cc.arccore.runtime.resource.ResourceType.EXECUTOR -> LeakType.ORPHAN_EXECUTOR
                        cc.arccore.runtime.resource.ResourceType.COROUTINE_SCOPE,
                        cc.arccore.runtime.resource.ResourceType.COROUTINE_JOB -> LeakType.ORPHAN_COROUTINE
                        cc.arccore.runtime.resource.ResourceType.SCHEDULER_TASK -> LeakType.ORPHAN_SCHEDULER_TASK
                        cc.arccore.runtime.resource.ResourceType.LISTENER -> LeakType.STALE_LISTENER
                        cc.arccore.runtime.resource.ResourceType.SERVICE -> LeakType.STALE_SERVICE
                        cc.arccore.runtime.resource.ResourceType.COMMAND -> LeakType.STALE_COMMAND
                        else -> LeakType.DANGLING_REGISTRY_ENTRY
                    },
                    severity = if (desc.type.isCritical) LeakSeverity.HIGH else LeakSeverity.MEDIUM,
                    description = "Orphan resource '${desc.name}' (${desc.type.name}) detected for module '${desc.moduleId}'"
                ))
            }
        )
        (resourceTracker as? cc.arccore.runtime.resource.DefaultResourceTracker)?.setLeakBridge(leakBridge)
    }

    override fun onEnable() {
        val result = runtime.startup()
        for (loadResult in result.loadResults) {
            if (loadResult is ModuleLoadResult.Success) {
                moduleManager.trackJarPath(
                    loadResult.container.module.id,
                    loadResult.jarPath
                )
            }
        }
        if (result.failedLoadCount > 0) {
            plugin.logger.warning("${result.failedLoadCount} module(s) failed to load")
        }
        if (result.failedEnableCount > 0) {
            plugin.logger.warning("${result.failedEnableCount} module(s) failed to enable")
        }
        plugin.logger.info(
            "ARCCore enabled: ${result.enabledCount}/${result.loadedCount} modules active"
        )

        val reloadCmd = ArcReloadCommand(
            hotReloadManager = hotReloadManager,
            moduleIdProvider = { runtime.getContainers().map { it.module.id }.toSet() }
        )
        val diagnosticsCmd = ArcDiagnosticsCommand(diagnosticsManager, resourceTracker)
        val leaksCmd = ArcLeaksCommand(leakDetectionManager)

        plugin.getCommand("arc")?.setExecutor { sender, command, label, args ->
            when (args.firstOrNull()?.lowercase()) {
                "reload" -> reloadCmd.onCommand(sender, command, label, args)
                "diagnostics" -> diagnosticsCmd.onCommand(sender, command, label, args)
                "leaks" -> leaksCmd.onCommand(sender, command, label, args)
                else -> {
                    sender.sendMessage("§7[ARCCore] Usage: /arc <reload|diagnostics|leaks>")
                    true
                }
            }
        }
        plugin.getCommand("arc")?.tabCompleter = TabCompleter { sender, command, alias, args ->
            when {
                args.isEmpty() || args.size == 1 ->
                    listOf("reload", "diagnostics", "leaks").filter { it.startsWith(args.firstOrNull()?.lowercase() ?: "") }
                args[0].lowercase() == "reload" -> reloadCmd.onTabComplete(sender, command, alias, args)
                args[0].lowercase() == "diagnostics" -> diagnosticsCmd.onTabComplete(sender, command, alias, args)
                args[0].lowercase() == "leaks" -> leaksCmd.onTabComplete(sender, command, alias, args)
                else -> emptyList()
            }
        }
    }

    override fun onDisable() {
        if (::leakDetectionManager.isInitialized) {
            leakDetectionManager.shutdown()
        }
        if (::diagnosticsManager.isInitialized) {
            diagnosticsManager.shutdown()
        }
        if (::runtime.isInitialized) {
            runtime.shutdown()
        }
    }
}
