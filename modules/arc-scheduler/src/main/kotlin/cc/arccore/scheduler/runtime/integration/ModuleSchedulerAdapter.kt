package cc.arccore.scheduler.runtime.integration

import cc.arccore.runtime.context.scheduler.ModuleScheduler

// 기존 ModuleScheduler를 SchedulerRuntime에서 사용하는 어댑터
// arc-runtime의 ModuleScheduler.ScheduledTaskHandle을 래핑

fun interface PlatformHandle {
    fun cancel()
}

interface ModuleSchedulerAdapter {
    fun runSync(action: () -> Unit): PlatformHandle
    fun runAsync(action: () -> Unit): PlatformHandle
    fun runLater(delayTicks: Long, action: () -> Unit): PlatformHandle
    fun runAsyncLater(delayTicks: Long, action: () -> Unit): PlatformHandle
    fun runRepeating(delayTicks: Long, periodTicks: Long, action: () -> Unit): PlatformHandle
    fun runAsyncRepeating(delayTicks: Long, periodTicks: Long, action: () -> Unit): PlatformHandle
    fun cancelAll()
}

// 기존 cc.arccore.runtime.context.scheduler.ModuleScheduler를 어댑팅
class BukkitModuleSchedulerAdapter(
    private val delegate: ModuleScheduler
) : ModuleSchedulerAdapter {

    override fun runSync(action: () -> Unit): PlatformHandle {
        val handle = delegate.runSync(Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun runAsync(action: () -> Unit): PlatformHandle {
        val handle = delegate.runAsync(Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun runLater(delayTicks: Long, action: () -> Unit): PlatformHandle {
        val handle = delegate.runLater(delayTicks, Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun runAsyncLater(delayTicks: Long, action: () -> Unit): PlatformHandle {
        val handle = delegate.runAsyncLater(delayTicks, Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun runRepeating(delayTicks: Long, periodTicks: Long, action: () -> Unit): PlatformHandle {
        val handle = delegate.runRepeating(delayTicks, periodTicks, Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun runAsyncRepeating(delayTicks: Long, periodTicks: Long, action: () -> Unit): PlatformHandle {
        val handle = delegate.runAsyncRepeating(delayTicks, periodTicks, Runnable { action() })
        return PlatformHandle { handle.cancel() }
    }

    override fun cancelAll() = delegate.cancelAll()
}
