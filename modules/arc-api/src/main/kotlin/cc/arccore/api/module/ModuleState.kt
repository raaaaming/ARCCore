package cc.arccore.api.module

enum class ModuleState {
    CREATED,
    LOADED,
    ENABLING,
    ENABLED,
    DISABLING,
    DISABLED,
    UNLOADING,
    UNLOADED,
    FAILED;

    fun canTransitionTo(next: ModuleState): Boolean {
        return when (this) {
            CREATED -> next == LOADED || next == FAILED
            LOADED -> next == ENABLING || next == UNLOADING || next == FAILED
            ENABLING -> next == ENABLED || next == LOADED || next == FAILED
            ENABLED -> next == DISABLING || next == FAILED
            DISABLING -> next == DISABLED || next == ENABLED || next == FAILED
            DISABLED -> next == ENABLING || next == UNLOADING || next == FAILED
            UNLOADING -> next == UNLOADED || next == DISABLED || next == FAILED
            UNLOADED -> false
            FAILED -> next == UNLOADING || next == FAILED
        }
    }
}
