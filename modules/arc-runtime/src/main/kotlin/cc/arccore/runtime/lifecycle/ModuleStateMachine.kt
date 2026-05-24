package cc.arccore.runtime.lifecycle

import cc.arccore.api.module.ModuleState

object ModuleStateMachine {

    val INTERMEDIATE_STATES: Set<ModuleState> = setOf(
        ModuleState.ENABLING,
        ModuleState.DISABLING,
        ModuleState.UNLOADING
    )

    val STABLE_STATES: Set<ModuleState> = setOf(
        ModuleState.CREATED,
        ModuleState.LOADED,
        ModuleState.ENABLED,
        ModuleState.DISABLED,
        ModuleState.UNLOADED,
        ModuleState.FAILED
    )

    fun isIntermediate(state: ModuleState): Boolean = state in INTERMEDIATE_STATES

    fun isStable(state: ModuleState): Boolean = state in STABLE_STATES

    fun isTerminal(state: ModuleState): Boolean =
        state == ModuleState.UNLOADED

    fun canEnable(state: ModuleState): Boolean =
        state.canTransitionTo(ModuleState.ENABLING)

    fun canDisable(state: ModuleState): Boolean =
        state.canTransitionTo(ModuleState.DISABLING)

    fun canUnload(state: ModuleState): Boolean =
        state.canTransitionTo(ModuleState.UNLOADING)

    fun requiresRollback(state: ModuleState): Boolean =
        state == ModuleState.ENABLING
            || state == ModuleState.DISABLING
            || state == ModuleState.UNLOADING

    fun rollbackTarget(state: ModuleState): ModuleState? {
        return when (state) {
            ModuleState.ENABLING -> ModuleState.LOADED
            ModuleState.DISABLING -> ModuleState.ENABLED
            ModuleState.UNLOADING -> ModuleState.DISABLED
            else -> null
        }
    }
}
