package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.coordination.ShadowModuleRegistry
import cc.arccore.zerodowntime.runtime.hotswap.ZeroDowntimeReadinessProbe
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.validation.NewRuntimeValidator

internal class ValidateStage(
    private val shadowRegistry: ShadowModuleRegistry,
    private val validator: NewRuntimeValidator = NewRuntimeValidator()
) : PipelineStage {
    override val phase = ZeroDowntimePhase.VALIDATE
    override val rollbackOnFailure = true

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.VALIDATE

        val newModule = shadowRegistry.get(context.targetModuleId)
            ?: return StageResult.Failure(IllegalStateException("New module not found in shadow registry"))

        val report = validator.validateBasic(context.targetModuleId)
        if (!report.compatible) {
            val errors = report.errors.joinToString(", ")
            return StageResult.Failure(IllegalStateException("New module validation failed: $errors"))
        }

        if (newModule is ZeroDowntimeReadinessProbe) {
            return when (val readiness = validator.checkReadinessProbe(newModule, newModule.readinessTimeoutMs)) {
                is NewRuntimeValidator.ReadinessCheckResult.Ready -> StageResult.Success
                is NewRuntimeValidator.ReadinessCheckResult.NotReady ->
                    StageResult.Failure(IllegalStateException("New module not ready: ${readiness.reason}"))
                is NewRuntimeValidator.ReadinessCheckResult.Failed ->
                    StageResult.Failure(readiness.cause)
            }
        }

        return StageResult.Success
    }
}
