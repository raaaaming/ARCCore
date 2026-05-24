package cc.arccore.scheduler.runtime

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.cancellation.CancellationTracker
import cc.arccore.scheduler.runtime.coroutine.CoroutineSchedulerBridge
import cc.arccore.scheduler.runtime.diagnostics.SchedulerDiagnosticsCollector
import cc.arccore.scheduler.runtime.exception.ModuleUnloadedException
import cc.arccore.scheduler.runtime.integration.ModuleSchedulerAdapter
import cc.arccore.scheduler.runtime.integration.PlatformHandle
import cc.arccore.scheduler.runtime.ownership.TaskOwnershipRegistry
import cc.arccore.scheduler.runtime.scheduling.TickDuration
import cc.arccore.scheduler.runtime.state.SchedulerStateRegistry
import cc.arccore.scheduler.runtime.task.CoroutineTaskHandle
import cc.arccore.scheduler.runtime.task.RepeatingTaskHandle
import cc.arccore.scheduler.runtime.task.ScheduledTask
import cc.arccore.scheduler.runtime.task.ScheduledTaskType
import cc.arccore.scheduler.runtime.task.TaskHandle
import cc.arccore.scheduler.runtime.validation.TaskSchedulingValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

class DefaultSchedulerRuntime internal constructor(
    override val moduleId: String,
    private val adapter: ModuleSchedulerAdapter,
    private val ownershipRegistry: TaskOwnershipRegistry,
    private val stateRegistry: SchedulerStateRegistry,
    private val cancellationTracker: CancellationTracker,
    private val diagnostics: SchedulerDiagnosticsCollector,
    private val validator: TaskSchedulingValidator = TaskSchedulingValidator(),
    private val coroutineAdapter: CoroutineSchedulerBridge? = null
) : SchedulerRuntime {

    init {
        stateRegistry.initialize(moduleId)
        stateRegistry.activate(moduleId)
    }

    override fun sync(task: () -> Unit): ScheduledTask {
        ensureCanSchedule()
        // AtomicReference 홀더로 단일 TaskHandle이 platformCancelFn을 참조하도록 보장
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = TaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.SYNC_ONCE,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runSync {
            handle.markRunning()
            try { task() } finally { handle.markCompleted() }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    override fun async(task: suspend () -> Unit): ScheduledTask {
        ensureCanSchedule()
        if (coroutineAdapter != null) {
            return scheduleCoroutineOnce(TickDuration.ZERO, task)
        }
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = TaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.ASYNC_ONCE,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runAsync {
            handle.markRunning()
            try {
                runBlocking { task() }
            } finally { handle.markCompleted() }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    override fun delay(duration: TickDuration, task: () -> Unit): ScheduledTask {
        ensureCanSchedule()
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = TaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.SYNC_DELAYED,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runLater(duration.ticks) {
            handle.markRunning()
            try { task() } finally { handle.markCompleted() }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    override fun delayAsync(duration: TickDuration, task: suspend () -> Unit): ScheduledTask {
        ensureCanSchedule()
        if (coroutineAdapter != null) {
            return scheduleCoroutineOnce(duration, task)
        }
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = TaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.ASYNC_DELAYED,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runAsyncLater(duration.ticks) {
            handle.markRunning()
            try { runBlocking { task() } } finally { handle.markCompleted() }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    override fun repeat(delay: TickDuration, period: TickDuration, task: () -> Unit): ScheduledTask {
        ensureCanSchedule()
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = RepeatingTaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.SYNC_REPEATING,
            delay = delay,
            period = period,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runRepeating(delay.ticks, period.ticks) {
            if (handle.isActive()) {
                handle.onExecute()
                try { task() } catch (_: Exception) {}
            }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    override fun repeatAsync(delay: TickDuration, period: TickDuration, task: suspend () -> Unit): ScheduledTask {
        ensureCanSchedule()
        if (coroutineAdapter != null) {
            return scheduleCoroutineRepeating(delay, period, task)
        }
        val platformHandleRef = AtomicReference<PlatformHandle?>(null)
        val handle = RepeatingTaskHandle(
            moduleId = moduleId,
            type = ScheduledTaskType.ASYNC_REPEATING,
            delay = delay,
            period = period,
            platformCancelFn = { platformHandleRef.get()?.cancel() }
        )
        val platformHandle = adapter.runAsyncRepeating(delay.ticks, period.ticks) {
            if (handle.isActive()) {
                handle.onExecute()
                try { runBlocking { task() } } catch (_: Exception) {}
            }
        }
        platformHandleRef.set(platformHandle)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    private fun scheduleCoroutineOnce(delay: TickDuration, task: suspend () -> Unit): ScheduledTask {
        val type = if (delay == TickDuration.ZERO) ScheduledTaskType.COROUTINE_ONCE else ScheduledTaskType.COROUTINE_DELAYED
        val handle = CoroutineTaskHandle(moduleId, type, delay)
        val job = coroutineAdapter!!.launch {
            if (delay != TickDuration.ZERO) {
                delay(delay.toMillis())
            }
            task()
        }
        handle.attachJob(job)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    private fun scheduleCoroutineRepeating(delay: TickDuration, period: TickDuration, task: suspend () -> Unit): ScheduledTask {
        val handle = CoroutineTaskHandle(moduleId, ScheduledTaskType.COROUTINE_REPEATING, delay, period)
        val job = coroutineAdapter!!.launch {
            if (delay != TickDuration.ZERO) {
                delay(delay.toMillis())
            }
            while (isActive && handle.isActive()) {
                val startMs = System.currentTimeMillis()
                try { task() } catch (_: Exception) {}
                val elapsed = System.currentTimeMillis() - startMs
                val remaining = period.toMillis() - elapsed
                if (remaining > 0) delay(remaining)
            }
        }
        handle.attachJob(job)
        ownershipRegistry.register(moduleId, handle)
        diagnostics.onTaskRegistered(handle)
        return handle
    }

    private fun ensureCanSchedule() {
        if (!stateRegistry.canSchedule(moduleId)) {
            throw ModuleUnloadedException(moduleId)
        }
    }

    override fun activeTasks(): List<ScheduledTask> = ownershipRegistry.getActiveTasks(moduleId)

    override fun activeTaskCount(): Int = ownershipRegistry.activeTaskCount(moduleId)

    override fun cancelAll() {
        ownershipRegistry.cancelAllForModule(moduleId, CancellationReason.MANUAL)
        cancellationTracker.record("*", moduleId, CancellationReason.MANUAL)
        diagnostics.onAllCancelled(moduleId)
    }
}
