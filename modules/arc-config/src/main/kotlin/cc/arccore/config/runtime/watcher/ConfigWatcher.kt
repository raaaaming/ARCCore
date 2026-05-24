package cc.arccore.config.runtime.watcher

interface ConfigWatcher : AutoCloseable {
    val watchedPath: String
    val isActive: Boolean
    fun start()
    fun stop()
}
