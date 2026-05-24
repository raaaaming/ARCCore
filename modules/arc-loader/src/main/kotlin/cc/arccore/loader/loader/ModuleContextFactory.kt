package cc.arccore.loader.loader

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.MutableModuleContext
import cc.arccore.loader.ModuleClassLoader
import java.nio.file.Path

interface ModuleContextFactory {
    fun create(
        api: ArcAPI,
        module: ArcModuleAPI,
        description: ModuleDescription,
        dataFolder: Path,
        classLoader: ModuleClassLoader?,
        owner: ModuleContainerView
    ): MutableModuleContext
}

class SimpleModuleContextFactory : ModuleContextFactory {
    override fun create(
        api: ArcAPI,
        module: ArcModuleAPI,
        description: ModuleDescription,
        dataFolder: Path,
        classLoader: ModuleClassLoader?,
        owner: ModuleContainerView
    ): MutableModuleContext = SimpleModuleContext(
        api = api,
        module = module,
        logger = SimpleModuleLogger(description.id),
        dataFolder = dataFolder,
        description = description,
        state = ModuleState.CREATED,
        cleanupScope = cc.arccore.loader.unload.DefaultCleanupScope(description.id),
        classLoader = classLoader
    )
}
