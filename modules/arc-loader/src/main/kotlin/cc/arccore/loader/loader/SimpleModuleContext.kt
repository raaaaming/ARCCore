package cc.arccore.loader.loader

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.api.module.CleanupScope
import cc.arccore.api.module.ConfigRuntimeMarker
import cc.arccore.api.module.MutableModuleContext
import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.ModuleLogger
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.StorageRuntime
import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.loader.ModuleClassLoader
import java.nio.file.Path

class SimpleModuleContext(
    override val api: ArcAPI,
    override val module: ArcModuleAPI,
    override val logger: ModuleLogger,
    override val dataFolder: Path,
    override val description: ModuleDescription,
    override var state: ModuleState,
    override val cleanupScope: CleanupScope,
    override val storage: StorageRuntime = StorageRuntime.NOOP,
    override val config: ConfigRuntimeMarker = ConfigRuntimeMarker.NOOP,
    private val classLoader: ModuleClassLoader? = null
) : MutableModuleContext, ClassLoaderHolder {

    override fun <T : Any> getService(type: Class<T>): T? =
        api.serviceRegistry.get(type.kotlin) ?: api.container.getOrNull(type)

    override fun updateState(newState: ModuleState) {
        state = newState
    }

    override fun provideClassLoader(): ClassLoader? = classLoader
}
