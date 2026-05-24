package cc.arccore.loader.unload

import cc.arccore.api.module.CleanupScope
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock

class DefaultCleanupScope(private val moduleId: String) : CleanupScope {

    private val log = Logger.getLogger(DefaultCleanupScope::class.java.name)
    private val lock = ReentrantLock()
    private val anonymousEntries = ArrayDeque<ScopeEntry>()
    private val namedEntries = LinkedHashMap<String, AutoCloseable>()
    private val _closed = AtomicBoolean(false)

    override val isClosed: Boolean get() = _closed.get()

    override fun register(closeable: AutoCloseable) {
        lock.withLock {
            check(!_closed.get()) { "CleanupScope for '$moduleId' is already closed" }
            anonymousEntries.addLast(ScopeEntry.Closeable(closeable))
        }
    }

    override fun onClose(action: () -> Unit) {
        lock.withLock {
            check(!_closed.get()) { "CleanupScope for '$moduleId' is already closed" }
            anonymousEntries.addLast(ScopeEntry.Lambda(action))
        }
    }

    override fun register(key: String, closeable: AutoCloseable) {
        lock.withLock {
            check(!_closed.get()) { "CleanupScope for '$moduleId' is already closed" }
            namedEntries.put(key, closeable)?.closeQuietly(key)
        }
    }

    override fun release(key: String) {
        lock.withLock {
            namedEntries.remove(key)?.closeQuietly(key)
        }
    }

    override fun close() {
        if (!_closed.compareAndSet(false, true)) return
        lock.withLock {
            val namedList = namedEntries.entries.toList().reversed()
            namedEntries.clear()
            val anonList = buildList {
                while (anonymousEntries.isNotEmpty()) add(anonymousEntries.removeLast())
            }
            namedList.forEach { (key, c) -> c.closeQuietly(key) }
            anonList.forEach { it.closeQuietly(log, moduleId) }
        }
    }

    private fun AutoCloseable.closeQuietly(key: String) {
        runCatching { close() }.onFailure {
            log.warning("[$moduleId] named resource '$key' close failed: ${it.message}")
        }
    }

    private sealed class ScopeEntry {
        abstract fun closeQuietly(log: Logger, moduleId: String)

        class Closeable(private val c: AutoCloseable) : ScopeEntry() {
            override fun closeQuietly(log: Logger, moduleId: String) =
                runCatching { c.close() }
                    .onFailure { log.warning("[$moduleId] resource close failed: ${it.message}") }
                    .let {}
        }

        class Lambda(private val fn: () -> Unit) : ScopeEntry() {
            override fun closeQuietly(log: Logger, moduleId: String) =
                runCatching { fn() }
                    .onFailure { log.warning("[$moduleId] onClose lambda failed: ${it.message}") }
                    .let {}
        }
    }
}
