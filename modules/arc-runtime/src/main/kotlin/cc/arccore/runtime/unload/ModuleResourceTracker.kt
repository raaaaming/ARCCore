package cc.arccore.runtime.unload

import cc.arccore.runtime.unload.cleanup.CleanupError
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ModuleResourceTracker(private val moduleId: String) {

    private val tracked = ConcurrentHashMap<String, TrackedResource>()
    private val disposed = AtomicBoolean(false)

    fun track(key: String, resource: AutoCloseable) {
        if (disposed.get()) return
        tracked[key] = TrackedResource(key, resource, null, System.nanoTime())
    }

    fun track(key: String, cleanup: () -> Unit) {
        if (disposed.get()) return
        tracked[key] = TrackedResource(key, null, cleanup, System.nanoTime())
    }

    fun untrack(key: String) {
        tracked.remove(key)
    }

    fun releaseAll(): List<CleanupError> {
        disposed.set(true)
        val errors = mutableListOf<CleanupError>()
        val keys = tracked.keys.sorted()
        for (key in keys) {
            val entry = tracked.remove(key) ?: continue
            try {
                entry.close()
            } catch (e: Exception) {
                errors.add(CleanupError(key, e))
            }
        }
        return errors
    }

    fun getTrackedCount(): Int = tracked.size

    fun getTrackedKeys(): Set<String> = tracked.keys.toSet()

    data class TrackedResource(
        val key: String,
        private val closeable: AutoCloseable?,
        private val cleanupFn: (() -> Unit)?,
        val createdAtNanos: Long
    ) {
        constructor(key: String, closeable: AutoCloseable, createdAtNanos: Long) :
            this(key, closeable, null, createdAtNanos)

        constructor(key: String, cleanupFn: () -> Unit, createdAtNanos: Long) :
            this(key, null, cleanupFn, createdAtNanos)

        fun close() {
            closeable?.close()
            cleanupFn?.invoke()
        }
    }
}
