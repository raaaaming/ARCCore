package cc.arccore.zerodowntime.runtime.transition

import cc.arccore.zerodowntime.runtime.coordination.GenerationCounter
import cc.arccore.zerodowntime.runtime.coordination.RoutingCoordinator
import cc.arccore.zerodowntime.runtime.coordination.ShadowModuleRegistry
import cc.arccore.zerodowntime.runtime.model.RuntimeRole
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.synchronization.ReadWriteGate

internal class SwitchRoutingStage(
    private val routingCoordinator: RoutingCoordinator,
    private val shadowRegistry: ShadowModuleRegistry,
    private val generationCounter: GenerationCounter,
    private val gateProvider: (String) -> ReadWriteGate = { ReadWriteGate(it) }
) : PipelineStage {
    override val phase = ZeroDowntimePhase.SWITCH_ROUTING
    override val rollbackOnFailure = true

    override fun execute(context: TransitionContext): StageResult {
        context.phase = ZeroDowntimePhase.SWITCH_ROUTING

        val gate = gateProvider(context.targetModuleId)

        gate.acquireWrite().use {
            routingCoordinator.switchToPrimary(
                context.targetModuleId,
                "${context.targetModuleId}-new",
                context
            )

            generationCounter.increment(context.targetModuleId)

            shadowRegistry.promote(context.targetModuleId)

            context.newHandle = context.newHandle?.copy(role = RuntimeRole.PRIMARY)
        }

        return StageResult.Success
    }
}
