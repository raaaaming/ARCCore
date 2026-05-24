package cc.arccore.api.lifecycle

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleState

data class LifecycleEvent(
    val container: ModuleContainerView,
    val type: LifecycleEventType,
    val previousState: ModuleState,
    val currentState: ModuleState,
    val cause: Throwable? = null,
    val timestamp: Long = System.nanoTime()
)
