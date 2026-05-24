package cc.arccore.config.runtime.watcher

/**
 * Placeholder [ConfigWatcher] that does nothing.
 *
 * Used until a real filesystem-watcher integration is wired in. Keeps the
 * watcher registry functional with a safe, inert implementation.
 */
class NoopConfigWatcher(override val watchedPath: String) : ConfigWatcher {
    override val isActive: Boolean get() = false
    override fun start() {}
    override fun stop() {}
    override fun close() {}
}
