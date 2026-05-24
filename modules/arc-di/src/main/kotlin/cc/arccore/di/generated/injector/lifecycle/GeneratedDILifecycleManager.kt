package cc.arccore.di.generated.injector.lifecycle

import cc.arccore.api.lifecycle.FilteredLifecycleObserver
import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.di.container.DefaultDIContainer

/**
 * Wires the generated DI system into the module lifecycle.
 *
 * On ENABLED: loads generated injectors from the module's ClassLoader and
 * registers them in a fresh [cc.arccore.di.generated.injector.GeneratedObjectGraph]
 * inside [DefaultDIContainer].
 *
 * On UNLOADED / FAILED / DEPENDENCY_FAILED: tears down the graph,
 * invoking [cc.arccore.api.di.generated.GeneratedInjector.cleanup] on every
 * cached instance. This guarantees no leaked object holds a reference into
 * the unloaded ClassLoader, making hot-reload safe.
 *
 * Register this observer alongside [cc.arccore.di.lifecycle.DILifecycleManager]
 * in the module bootstrap so both the reflection-based and generated DI paths
 * are cleaned up on unload.
 */
class GeneratedDILifecycleManager(
    private val container: DefaultDIContainer
) : FilteredLifecycleObserver(
    LifecycleEventType.ENABLED,
    LifecycleEventType.UNLOADED,
    LifecycleEventType.FAILED,
    LifecycleEventType.DEPENDENCY_FAILED
) {

    override fun onAcceptedEvent(event: LifecycleEvent) {
        val moduleId = event.container.module.id
        when (event.type) {
            LifecycleEventType.ENABLED -> {
                val classLoader = (event.container.context as? ClassLoaderHolder)
                    ?.provideClassLoader() ?: return
                container.initializeGeneratedGraph(moduleId, classLoader)
            }
            else -> container.cleanupGeneratedGraph(moduleId)
        }
    }
}
