package cc.arccore.runtime.context

import cc.arccore.api.ArcAPI
import cc.arccore.api.module.ArcModuleAPI
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.api.module.CleanupScope
import cc.arccore.api.module.ModuleDescription
import cc.arccore.api.module.ModuleLogger
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.ConfigRuntimeMarker
import cc.arccore.api.module.StorageRuntime
import cc.arccore.runtime.context.access.ScopedCommandAccess
import cc.arccore.runtime.context.access.ScopedListenerAccess
import cc.arccore.runtime.context.access.ScopedServiceAccess
import cc.arccore.runtime.context.async.AsyncRuntimeAccess
import cc.arccore.runtime.context.lifecycle.LifecycleGuard
import cc.arccore.runtime.context.scheduler.ModuleScheduler
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

class DefaultModuleContext(
    override val api: ArcAPI,
    override val module: ArcModuleAPI,
    override val logger: ModuleLogger,
    override val dataFolder: Path,
    override val description: ModuleDescription,
    initialState: ModuleState,
    override val cleanupScope: CleanupScope,
    private val guard: LifecycleGuard,
    override val scheduler: ModuleScheduler,
    override val services: ScopedServiceAccess,
    override val commands: ScopedCommandAccess,
    override val listeners: ScopedListenerAccess,
    override val runtime: RuntimeFacade,
    override val asyncRuntime: AsyncRuntimeAccess? = null,
    override val storage: StorageRuntime = StorageRuntime.NOOP,
    override val config: ConfigRuntimeMarker = ConfigRuntimeMarker.NOOP,
    private val classLoader: ClassLoader? = null
) : RuntimeModuleContext, ClassLoaderHolder {

    private val _state = AtomicReference(initialState)

    override val state: ModuleState get() = _state.get()

    override val isValid: Boolean get() = guard.isContextValid(this)

    override fun updateState(newState: ModuleState) {
        _state.set(newState)
    }

    override fun <T : Any> getService(type: Class<T>): T? = api.serviceRegistry.get(type.kotlin)

    override fun provideClassLoader(): ClassLoader? = classLoader
}
