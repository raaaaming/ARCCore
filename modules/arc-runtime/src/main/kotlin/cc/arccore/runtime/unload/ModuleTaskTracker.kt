package cc.arccore.runtime.unload

import cc.arccore.runtime.unload.cleanup.CleanupError
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ModuleTaskTracker(private val moduleId: String) {

    private val scheduledTasks = CopyOnWriteArrayList<ScheduledTaskHandle>()
    private val executors = CopyOnWriteArrayList<ExecutorService>()
    private val threads = CopyOnWriteArrayList<Thread>()
    private val disposed = AtomicBoolean(false)

    fun trackScheduledTask(handle: ScheduledTaskHandle) {
        if (disposed.get()) return
        scheduledTasks.add(handle)
    }

    fun trackExecutor(executor: ExecutorService) {
        if (disposed.get()) return
        executors.add(executor)
    }

    fun trackThread(thread: Thread) {
        if (disposed.get()) return
        threads.add(thread)
    }

    fun cancelAllTasks(): List<CleanupError> {
        val errors = mutableListOf<CleanupError>()

        for (task in scheduledTasks) {
            try {
                task.cancel()
            } catch (e: Exception) {
                errors.add(CleanupError("task:${task.description()}", e))
            }
        }
        scheduledTasks.clear()

        for (executor in executors) {
            try {
                executor.shutdown()
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: Exception) {
                errors.add(CleanupError("executor:${executor}", e))
            }
        }
        executors.clear()

        disposed.set(true)
        return errors
    }

    fun getTrackedTaskCount(): Int = scheduledTasks.size
    fun getTrackedExecutorCount(): Int = executors.size
}

fun interface ScheduledTaskHandle {
    fun cancel()
    fun description(): String = "ScheduledTask"
}

class BukkitTaskHandle(
    private val task: Any,
    private val cancelFn: (Any) -> Unit
) : ScheduledTaskHandle {
    override fun cancel() = cancelFn(task)
    override fun description(): String = task::class.java.name
}
