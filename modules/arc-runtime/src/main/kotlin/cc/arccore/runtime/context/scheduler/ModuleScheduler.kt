package cc.arccore.runtime.context.scheduler

import cc.arccore.runtime.unload.ScheduledTaskHandle

interface ModuleScheduler {
    fun runLater(delayTicks: Long, action: Runnable): ScheduledTaskHandle
    fun runSync(action: Runnable): ScheduledTaskHandle
    fun runRepeating(delayTicks: Long, periodTicks: Long, action: Runnable): ScheduledTaskHandle
    fun runAsync(action: Runnable): ScheduledTaskHandle
    fun runAsyncLater(delayTicks: Long, action: Runnable): ScheduledTaskHandle
    fun runAsyncRepeating(delayTicks: Long, periodTicks: Long, action: Runnable): ScheduledTaskHandle
    fun cancelAll()
    fun activeTaskCount(): Int = 0
}
