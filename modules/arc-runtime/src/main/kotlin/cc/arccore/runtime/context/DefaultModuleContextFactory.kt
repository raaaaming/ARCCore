package cc.arccore.runtime.context

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.MutableModuleContext
import cc.arccore.loader.ModuleClassLoader
import cc.arccore.loader.loader.ModuleContextFactory
import cc.arccore.runtime.context.access.BukkitCommandBridge
import cc.arccore.loader.loader.SimpleModuleLogger
import cc.arccore.loader.unload.DefaultCleanupScope
import cc.arccore.runtime.context.access.DefaultScopedCommandAccess
import cc.arccore.runtime.context.access.DefaultScopedListenerAccess
import cc.arccore.runtime.context.access.DefaultScopedServiceAccess
import cc.arccore.runtime.context.async.AsyncRuntimeAccess
import cc.arccore.runtime.context.async.NoOpAsyncRuntimeAccess
import cc.arccore.runtime.context.cleanup.ContextCleanupCoordinator
import cc.arccore.runtime.context.lifecycle.DefaultLifecycleGuard
import cc.arccore.runtime.context.lifecycle.LifecycleGuard
import cc.arccore.runtime.context.scheduler.BukkitModuleScheduler
import cc.arccore.runtime.lifecycle.DefaultModuleLifecycleManager
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import java.nio.file.Path

class DefaultModuleContextFactory(
    private val plugin: Plugin,
    private val bukkit: BukkitScheduler,
    private val lifecycleManager: DefaultModuleLifecycleManager,
    private val guard: LifecycleGuard = DefaultLifecycleGuard(),
    private val asyncRuntimeFactory: ((String) -> AsyncRuntimeAccess)? = null
) : ModuleContextFactory {

    override fun create(
        api: ArcAPI,
        module: ArcModuleAPI,
        description: ModuleDescription,
        dataFolder: Path,
        classLoader: ModuleClassLoader?,
        owner: ModuleContainerView
    ): MutableModuleContext {
        val moduleId = description.id
        val taskTracker = lifecycleManager.getTaskTracker(moduleId)
        val resourceTracker = lifecycleManager.getResourceTracker(moduleId)
        val cleanupScope = DefaultCleanupScope(moduleId)

        val scheduler = BukkitModuleScheduler(plugin, bukkit, taskTracker)
        val services = DefaultScopedServiceAccess(api.serviceRegistry, owner, cleanupScope)
        val commands = DefaultScopedCommandAccess(
            api.commandRegistry, owner, cleanupScope,
            BukkitCommandBridge(plugin)
        )
        val listeners = DefaultScopedListenerAccess(cleanupScope)
        val ownershipTracker = lifecycleManager.getOwnershipTracker()
        val facade = DefaultRuntimeFacade(owner, taskTracker, resourceTracker, ownershipTracker)

        val asyncRuntime: AsyncRuntimeAccess = asyncRuntimeFactory?.invoke(moduleId) ?: NoOpAsyncRuntimeAccess
        if (asyncRuntime !is NoOpAsyncRuntimeAccess) {
            cleanupScope.register("async-runtime", AutoCloseable { asyncRuntime.close() })
        }

        return DefaultModuleContext(
            api = api,
            module = module,
            logger = SimpleModuleLogger(moduleId),
            dataFolder = dataFolder,
            description = description,
            initialState = ModuleState.CREATED,
            cleanupScope = cleanupScope,
            guard = guard,
            scheduler = scheduler,
            services = services,
            commands = commands,
            listeners = listeners,
            runtime = facade,
            asyncRuntime = asyncRuntime,
            classLoader = classLoader
        )
    }
}
