package cc.arccore.scheduler.runtime.ownership

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.task.CoroutineTaskHandle
import cc.arccore.scheduler.runtime.task.RepeatingTaskHandle
import cc.arccore.scheduler.runtime.task.ScheduledTask
import cc.arccore.scheduler.runtime.task.TaskHandle
import java.util.concurrent.ConcurrentHashMap

internal class TaskOwnershipRegistry {
    private val tasks = ConcurrentHashMap<String, MutableSet<ScheduledTask>>()

    fun register(moduleId: String, task: ScheduledTask) {
        tasks.computeIfAbsent(moduleId) { ConcurrentHashMap.newKeySet() }.add(task)
    }

    fun unregister(moduleId: String, task: ScheduledTask) {
        tasks[moduleId]?.remove(task)
    }

    fun getActiveTasks(moduleId: String): List<ScheduledTask> =
        tasks[moduleId]?.filter { it.isActive() } ?: emptyList()

    fun getAllTasks(moduleId: String): List<ScheduledTask> =
        tasks[moduleId]?.toList() ?: emptyList()

    fun cancelAllForModule(moduleId: String, reason: CancellationReason) {
        tasks[moduleId]?.forEach { task ->
            when (task) {
                is TaskHandle -> task.cancelWithReason(reason)
                is RepeatingTaskHandle -> task.cancelWithReason(reason)
                is CoroutineTaskHandle -> task.cancelWithReason(reason)
                else -> task.cancel()
            }
        }
        tasks.remove(moduleId)
    }

    fun activeTaskCount(moduleId: String): Int = getActiveTasks(moduleId).size

    fun totalTaskCount(moduleId: String): Int = tasks[moduleId]?.size ?: 0
}
