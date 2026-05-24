package cc.arccore.runtime.lifecycle.validation

import cc.arccore.api.module.ModuleContainer
import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.lifecycle.exception.LifecycleValidationException

object LifecycleValidator {

    fun validateCanEnable(container: ModuleContainer) {
        val state = container.state
        if (state != ModuleState.LOADED && state != ModuleState.DISABLED) {
            throw LifecycleValidationException(
                "Cannot enable module '${container.module.id}': " +
                    "current state is $state, expected LOADED or DISABLED"
            )
        }
    }

    fun validateCanDisable(container: ModuleContainer) {
        if (container.state != ModuleState.ENABLED) {
            throw LifecycleValidationException(
                "Cannot disable module '${container.module.id}': " +
                    "current state is ${container.state}, expected ENABLED"
            )
        }
    }

    fun validateCanUnload(container: ModuleContainer) {
        val state = container.state
        if (state != ModuleState.LOADED && state != ModuleState.DISABLED && state != ModuleState.FAILED) {
            throw LifecycleValidationException(
                "Cannot unload module '${container.module.id}': " +
                    "current state is $state, expected LOADED, DISABLED, or FAILED"
            )
        }
    }

    fun validateTransition(container: ModuleContainer, target: ModuleState) {
        if (!container.state.canTransitionTo(target)) {
            throw LifecycleValidationException(
                "Invalid state transition for module '${container.module.id}': " +
                    "${container.state} → $target"
            )
        }
    }
}
