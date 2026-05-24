package cc.arccore.runtime.context.scheduler

import cc.arccore.runtime.unload.ModuleTaskTracker
import cc.arccore.runtime.unload.ScheduledTaskHandle
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask

class BukkitModuleScheduler(
    private val plugin: Plugin,
    private val scheduler: BukkitScheduler,
    private val taskTracker: ModuleTaskTracker
) : ModuleScheduler {

    private fun BukkitTask.toHandle(): ScheduledTaskHandle = object : ScheduledTaskHandle {
        override fun cancel() = this@toHandle.cancel()
        override fun description() = "BukkitTask#${this@toHandle.taskId}"
    }

    private fun track(task: BukkitTask): ScheduledTaskHandle {
        val handle = task.toHandle()
        taskTracker.trackScheduledTask(handle)
        return handle
    }

    override fun runSync(action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTask(plugin, action))

    override fun runLater(delayTicks: Long, action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTaskLater(plugin, action, delayTicks))

    override fun runRepeating(delayTicks: Long, periodTicks: Long, action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTaskTimer(plugin, action, delayTicks, periodTicks))

    override fun runAsync(action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTaskAsynchronously(plugin, action))

    override fun runAsyncLater(delayTicks: Long, action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTaskLaterAsynchronously(plugin, action, delayTicks))

    override fun runAsyncRepeating(delayTicks: Long, periodTicks: Long, action: Runnable): ScheduledTaskHandle =
        track(scheduler.runTaskTimerAsynchronously(plugin, action, delayTicks, periodTicks))

    override fun cancelAll() {
        taskTracker.cancelAllTasks()
    }

    override fun activeTaskCount(): Int = taskTracker.getTrackedTaskCount()
}
