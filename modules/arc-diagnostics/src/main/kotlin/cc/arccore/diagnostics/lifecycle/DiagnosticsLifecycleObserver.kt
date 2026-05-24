package cc.arccore.diagnostics.lifecycle

import cc.arccore.api.lifecycle.FilteredLifecycleObserver
import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.diagnostics.tracking.DiagnosticsRegistry

class DiagnosticsLifecycleObserver(
    private val registry: DiagnosticsRegistry
) : FilteredLifecycleObserver(
    LifecycleEventType.LOADED,
    LifecycleEventType.ENABLED,
    LifecycleEventType.DISABLED,
    LifecycleEventType.UNLOADED,
    LifecycleEventType.FAILED,
    LifecycleEventType.DEPENDENCY_FAILED
) {
    override fun onAcceptedEvent(event: LifecycleEvent) {
        val id = event.container.module.id
        when (event.type) {
            LifecycleEventType.LOADED -> registry.onModuleLoaded(id)
            LifecycleEventType.ENABLED -> registry.onModuleEnabled(id)
            LifecycleEventType.DISABLED -> { /* entry retained, enabledAt cleared is OK */ }
            LifecycleEventType.UNLOADED -> registry.onModuleUnloaded(id)
            LifecycleEventType.FAILED,
            LifecycleEventType.DEPENDENCY_FAILED -> registry.onModuleFailed(id)
        }
    }
}
