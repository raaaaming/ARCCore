package cc.arccore.runtime.lifecycle.transition

import cc.arccore.api.module.ModuleState

data class ModuleTransition(
    val from: ModuleState,
    val to: ModuleState,
    val rollbackTarget: ModuleState? = null
) {
    val isValid: Boolean get() = from.canTransitionTo(to)

    val isIntermediate: Boolean get() = to == ModuleState.ENABLING
        || to == ModuleState.DISABLING
        || to == ModuleState.UNLOADING

    val isTransition: Boolean get() = from == ModuleState.ENABLING
        || from == ModuleState.DISABLING
        || from == ModuleState.UNLOADING

    companion object {
        fun enable(from: ModuleState): ModuleTransition {
            require(from.canTransitionTo(ModuleState.ENABLING)) {
                "Cannot enable from $from"
            }
            return ModuleTransition(
                from = from,
                to = ModuleState.ENABLED,
                rollbackTarget = from
            )
        }

        fun disable(from: ModuleState): ModuleTransition {
            require(from.canTransitionTo(ModuleState.DISABLING)) {
                "Cannot disable from $from"
            }
            return ModuleTransition(
                from = from,
                to = ModuleState.DISABLED,
                rollbackTarget = from
            )
        }

        fun unload(from: ModuleState): ModuleTransition {
            require(from.canTransitionTo(ModuleState.UNLOADING)) {
                "Cannot unload from $from"
            }
            return ModuleTransition(
                from = from,
                to = ModuleState.UNLOADED,
                rollbackTarget = from
            )
        }

        val ENABLE_SEQUENCE = listOf(
            ModuleTransition(ModuleState.LOADED, ModuleState.ENABLING, ModuleState.LOADED),
            ModuleTransition(ModuleState.ENABLING, ModuleState.ENABLED, ModuleState.LOADED),
            ModuleTransition(ModuleState.DISABLED, ModuleState.ENABLING, ModuleState.DISABLED),
            ModuleTransition(ModuleState.ENABLING, ModuleState.ENABLED, ModuleState.DISABLED)
        )

        val DISABLE_SEQUENCE = listOf(
            ModuleTransition(ModuleState.ENABLED, ModuleState.DISABLING, ModuleState.ENABLED),
            ModuleTransition(ModuleState.DISABLING, ModuleState.DISABLED, ModuleState.ENABLED)
        )

        val UNLOAD_SEQUENCE = listOf(
            ModuleTransition(ModuleState.LOADED, ModuleState.UNLOADING, ModuleState.LOADED),
            ModuleTransition(ModuleState.UNLOADING, ModuleState.UNLOADED, ModuleState.DISABLED),
            ModuleTransition(ModuleState.DISABLED, ModuleState.UNLOADING, ModuleState.DISABLED),
            ModuleTransition(ModuleState.UNLOADING, ModuleState.UNLOADED, ModuleState.DISABLED),
            ModuleTransition(ModuleState.FAILED, ModuleState.UNLOADING, ModuleState.FAILED),
            ModuleTransition(ModuleState.UNLOADING, ModuleState.UNLOADED, ModuleState.FAILED)
        )
    }
}
