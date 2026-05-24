package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.rollback.ZeroDowntimeRollbackSnapshot
import cc.arccore.zerodowntime.runtime.state.TransitionStateRegistry
import cc.arccore.zerodowntime.runtime.validation.ZDTValidationFailure
import cc.arccore.zerodowntime.runtime.validation.ZeroDowntimeValidator

internal class PrepareStage(
    private val transitionRegistry: TransitionStateRegistry
) : PipelineStage {
    override val phase = ZeroDowntimePhase.PREPARE
    override val rollbackOnFailure = true

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.PREPARE

        val validationError = ZeroDowntimeValidator.validate(
            context.targetModuleId,
            transitionRegistry
        )

        if (validationError != null) {
            val reason = when (validationError) {
                is ZDTValidationFailure.ActiveTransitionExists ->
                    "Module '${validationError.moduleId}' is already transitioning"
                is ZDTValidationFailure.ModuleNotFound ->
                    "Module '${validationError.moduleId}' not found"
                is ZDTValidationFailure.DeferredStrategy ->
                    "Module uses DEFERRED hot-swap strategy"
                is ZDTValidationFailure.GenericValidationFailed ->
                    validationError.reason
            }
            return StageResult.Failure(IllegalStateException(reason))
        }

        context.rollbackSnapshot = ZeroDowntimeRollbackSnapshot(
            moduleId = context.targetModuleId,
            oldGeneration = context.oldHandle.generation,
            capturedModuleStates = context.capturedStates.toMap(),
            dependentModuleIds = context.affectedModuleIds
        )

        return StageResult.Success
    }
}
