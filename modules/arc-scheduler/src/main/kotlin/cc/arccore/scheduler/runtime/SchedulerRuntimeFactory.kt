package cc.arccore.scheduler.runtime

import cc.arccore.runtime.context.scheduler.ModuleScheduler
import cc.arccore.scheduler.runtime.cancellation.CancellationTracker
import cc.arccore.scheduler.runtime.coroutine.CoroutineSchedulerBridge
import cc.arccore.scheduler.runtime.diagnostics.SchedulerDiagnosticsCollector
import cc.arccore.scheduler.runtime.integration.BukkitModuleSchedulerAdapter
import cc.arccore.scheduler.runtime.integration.ContextSchedulerAccessor
import cc.arccore.scheduler.runtime.integration.ModuleSchedulerAdapter
import cc.arccore.scheduler.runtime.ownership.OrphanTaskDetector
import cc.arccore.scheduler.runtime.ownership.TaskOwnershipRegistry
import cc.arccore.scheduler.runtime.state.SchedulerStateRegistry
import cc.arccore.scheduler.runtime.validation.TaskSchedulingValidator

object SchedulerRuntimeFactory {
    private val ownershipRegistry = TaskOwnershipRegistry()
    private val stateRegistry = SchedulerStateRegistry()
    private val cancellationTracker = CancellationTracker()
    private val orphanDetector = OrphanTaskDetector(ownershipRegistry)
    private val diagnostics = SchedulerDiagnosticsCollector(ownershipRegistry, orphanDetector)

    fun create(
        moduleId: String,
        adapter: ModuleSchedulerAdapter,
        coroutineBridge: CoroutineSchedulerBridge? = null
    ): DefaultSchedulerRuntime {
        val runtime = DefaultSchedulerRuntime(
            moduleId = moduleId,
            adapter = adapter,
            ownershipRegistry = ownershipRegistry,
            stateRegistry = stateRegistry,
            cancellationTracker = cancellationTracker,
            diagnostics = diagnostics,
            validator = TaskSchedulingValidator(),
            coroutineAdapter = coroutineBridge
        )
        ContextSchedulerAccessor.register(moduleId, runtime)
        return runtime
    }

    // 기존 ModuleScheduler에서 생성
    fun createFromModuleScheduler(
        moduleId: String,
        moduleScheduler: ModuleScheduler,
        coroutineBridge: CoroutineSchedulerBridge? = null
    ): DefaultSchedulerRuntime {
        val adapter = BukkitModuleSchedulerAdapter(moduleScheduler)
        return create(moduleId, adapter, coroutineBridge)
    }

    internal fun getOwnershipRegistry(): TaskOwnershipRegistry = ownershipRegistry
    internal fun getStateRegistry(): SchedulerStateRegistry = stateRegistry
    internal fun getDiagnostics(): SchedulerDiagnosticsCollector = diagnostics
    internal fun getCancellationTracker(): CancellationTracker = cancellationTracker
    internal fun getOrphanDetector(): OrphanTaskDetector = orphanDetector
}
