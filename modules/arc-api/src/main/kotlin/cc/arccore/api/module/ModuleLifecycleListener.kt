package cc.arccore.api.module

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.lifecycle.LifecycleObserver

@Deprecated(
    message = "Use cc.arccore.api.lifecycle.LifecycleObserver instead.",
    level = DeprecationLevel.WARNING
)
interface ModuleLifecycleListener {

    fun onModuleLoad(module: ArcModuleAPI) {}

    fun onModuleEnable(module: ArcModuleAPI) {}

    fun onModuleDisable(module: ArcModuleAPI) {}

    fun onModuleUnload(module: ArcModuleAPI) {}

    fun onModuleFail(module: ArcModuleAPI, error: Throwable) {}

    fun asObserver(): LifecycleObserver = LifecycleObserver { event ->
        when (event.type) {
            LifecycleEventType.LOADED            -> onModuleLoad(event.container.module)
            LifecycleEventType.ENABLED           -> onModuleEnable(event.container.module)
            LifecycleEventType.DISABLED          -> onModuleDisable(event.container.module)
            LifecycleEventType.UNLOADED          -> onModuleUnload(event.container.module)
            LifecycleEventType.FAILED            ->
                onModuleFail(event.container.module, event.cause ?: RuntimeException("Unknown failure"))
            LifecycleEventType.DEPENDENCY_FAILED ->
                onModuleFail(event.container.module, event.cause ?: RuntimeException("Dependency failed"))
        }
    }
}
