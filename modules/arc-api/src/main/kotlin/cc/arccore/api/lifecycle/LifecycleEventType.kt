package cc.arccore.api.lifecycle

enum class LifecycleEventType {
    LOADED,
    ENABLED,
    DISABLED,
    UNLOADED,
    FAILED,
    DEPENDENCY_FAILED
}
