package cc.arccore.runtime.context.cleanup

import cc.arccore.api.module.CleanupScope
import cc.arccore.runtime.unload.ModuleResourceTracker
import cc.arccore.runtime.unload.ModuleTaskTracker
import cc.arccore.runtime.unload.cleanup.CleanupError
import java.util.concurrent.atomic.AtomicBoolean

class ContextCleanupCoordinator(
    val cleanupScope: CleanupScope,
    val taskTracker: ModuleTaskTracker,
    val resourceTracker: ModuleResourceTracker
) : AutoCloseable {

    private val _closed = AtomicBoolean(false)
    val isClosed: Boolean get() = _closed.get()

    override fun close() {
        closeAndReport()
    }

    fun closeAndReport(): CleanupReport {
        if (!_closed.compareAndSet(false, true)) return CleanupReport(emptyList(), emptyList(), null)

        val taskErrors = taskTracker.cancelAllTasks()
        val resourceErrors = resourceTracker.releaseAll()
        val scopeError = runCatching { cleanupScope.close() }.exceptionOrNull()

        return CleanupReport(taskErrors, resourceErrors, scopeError)
    }
}

data class CleanupReport(
    val taskErrors: List<CleanupError>,
    val resourceErrors: List<CleanupError>,
    val scopeError: Throwable?
) {
    val hasErrors: Boolean get() = taskErrors.isNotEmpty() || resourceErrors.isNotEmpty() || scopeError != null
}
