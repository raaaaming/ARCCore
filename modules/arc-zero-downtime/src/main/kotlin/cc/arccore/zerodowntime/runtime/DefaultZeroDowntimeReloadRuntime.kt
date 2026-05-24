package cc.arccore.zerodowntime.runtime

import cc.arccore.zerodowntime.runtime.coordination.GenerationCounter
import cc.arccore.zerodowntime.runtime.coordination.RoutingCoordinator
import cc.arccore.zerodowntime.runtime.coordination.ShadowModuleRegistry
import cc.arccore.zerodowntime.runtime.diagnostics.ZeroDowntimeDiagnosticsCollector
import cc.arccore.zerodowntime.runtime.lifecycle.ZeroDowntimeLifecycleObserver
import cc.arccore.zerodowntime.runtime.metrics.ZeroDowntimeMetricsAccumulator
import cc.arccore.zerodowntime.runtime.model.RuntimeHandle
import cc.arccore.zerodowntime.runtime.model.RuntimeRole
import cc.arccore.zerodowntime.runtime.model.TransitionContext
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeMetrics
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimeReloadResult
import cc.arccore.zerodowntime.runtime.ownership.OwnershipTransferCoordinator
import cc.arccore.zerodowntime.runtime.rollback.ZeroDowntimeRollbackManager
import cc.arccore.zerodowntime.runtime.state.TransitionState
import cc.arccore.zerodowntime.runtime.state.TransitionStateRegistry
import cc.arccore.zerodowntime.runtime.synchronization.TransitionLock
import cc.arccore.zerodowntime.runtime.transition.BootstrapNewStage
import cc.arccore.zerodowntime.runtime.transition.CleanupOldStage
import cc.arccore.zerodowntime.runtime.transition.OwnershipTransferStage
import cc.arccore.zerodowntime.runtime.transition.PrepareStage
import cc.arccore.zerodowntime.runtime.transition.RequestDrainStage
import cc.arccore.zerodowntime.runtime.transition.SwitchRoutingStage
import cc.arccore.zerodowntime.runtime.transition.ValidateStage
import cc.arccore.zerodowntime.runtime.transition.ZeroDowntimePipeline
import java.util.concurrent.CopyOnWriteArrayList

class DefaultZeroDowntimeReloadRuntime internal constructor(
    private val moduleBootstrapper: BootstrapNewStage.ModuleBootstrapper,
    private val oldRuntimeCleaner: CleanupOldStage.OldRuntimeCleaner
) : ZeroDowntimeReloadRuntime {

    private val transitionRegistry = TransitionStateRegistry()
    private val shadowRegistry = ShadowModuleRegistry()
    private val generationCounter = GenerationCounter()
    private val transitionLock = TransitionLock()
    private val routingCoordinator = RoutingCoordinator()
    private val ownershipCoordinator = OwnershipTransferCoordinator()
    private val rollbackManager = ZeroDowntimeRollbackManager(shadowRegistry, routingCoordinator, ownershipCoordinator)
    private val metricsAccumulator = ZeroDowntimeMetricsAccumulator()
    private val observers = CopyOnWriteArrayList<ZeroDowntimeLifecycleObserver>()
    private val diagnosticsCollector = ZeroDowntimeDiagnosticsCollector(metricsAccumulator)

    init {
        observers.add(diagnosticsCollector)
    }

    override fun reload(moduleId: String): ZeroDowntimeReloadResult {
        if (transitionRegistry.isTransitioning(moduleId)) {
            return ZeroDowntimeReloadResult.AlreadyTransitioning(moduleId)
        }

        if (!transitionLock.tryAcquire(moduleId, 5000L)) {
            return ZeroDowntimeReloadResult.Rejected(moduleId, "Could not acquire transition lock within 5s")
        }

        try {
            val currentGeneration = generationCounter.current(moduleId)
            val context = TransitionContext(
                targetModuleId = moduleId,
                oldHandle = RuntimeHandle(
                    moduleId = moduleId,
                    generation = currentGeneration,
                    role = RuntimeRole.OLD
                )
            )

            if (!transitionRegistry.begin(context)) {
                return ZeroDowntimeReloadResult.AlreadyTransitioning(moduleId)
            }

            try {
                val pipeline = buildPipeline()
                return pipeline.execute(context)
            } finally {
                transitionRegistry.complete(moduleId)
            }
        } finally {
            transitionLock.release(moduleId)
        }
    }

    override fun reloadAll(moduleIds: List<String>): Map<String, ZeroDowntimeReloadResult> {
        return moduleIds.associateWith { reload(it) }
    }

    override fun isTransitioning(moduleId: String): Boolean =
        transitionRegistry.isTransitioning(moduleId)

    override fun getTransitionState(moduleId: String): TransitionState? =
        transitionRegistry.snapshot(moduleId)

    override fun getActiveTransitions(): List<TransitionState> =
        transitionRegistry.allSnapshots()

    override fun getMetrics(): ZeroDowntimeMetrics = metricsAccumulator.snapshot()

    override fun addObserver(observer: ZeroDowntimeLifecycleObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: ZeroDowntimeLifecycleObserver) {
        observers.remove(observer)
    }

    private fun buildPipeline(): ZeroDowntimePipeline {
        return ZeroDowntimePipeline(
            stages = listOf(
                PrepareStage(transitionRegistry),
                BootstrapNewStage(shadowRegistry, moduleBootstrapper),
                ValidateStage(shadowRegistry),
                OwnershipTransferStage(ownershipCoordinator),
                RequestDrainStage(),
                SwitchRoutingStage(routingCoordinator, shadowRegistry, generationCounter),
                CleanupOldStage(oldRuntimeCleaner)
            ),
            rollbackManager = rollbackManager,
            observers = observers.toList()
        )
    }
}
