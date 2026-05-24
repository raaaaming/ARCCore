package cc.arccore.zerodowntime.runtime.validation

import cc.arccore.zerodowntime.runtime.state.TransitionStateRegistry

sealed class ZDTValidationFailure {
    data class ActiveTransitionExists(val moduleId: String) : ZDTValidationFailure()
    data class ModuleNotFound(val moduleId: String) : ZDTValidationFailure()
    data class DeferredStrategy(val moduleId: String) : ZDTValidationFailure()
    data class GenericValidationFailed(val reason: String) : ZDTValidationFailure()
}

internal object ZeroDowntimeValidator {
    fun validate(
        moduleId: String,
        transitionRegistry: TransitionStateRegistry
    ): ZDTValidationFailure? {
        if (transitionRegistry.isTransitioning(moduleId)) {
            return ZDTValidationFailure.ActiveTransitionExists(moduleId)
        }
        return null
    }
}
