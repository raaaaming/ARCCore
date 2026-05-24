package cc.arccore.config.runtime.watcher

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry of active [ConfigWatcher] instances keyed by watched path.
 */
class ConfigWatcherRegistry {

    private val watchers: ConcurrentHashMap<String, ConfigWatcher> = ConcurrentHashMap()

    fun register(path: String, watcher: ConfigWatcher) {
        val existing = watchers.put(path, watcher)
        // Stop any displaced watcher
        try {
            existing?.stop()
            existing?.close()
        } catch (_: Exception) {}
    }

    fun get(path: String): ConfigWatcher? = watchers[path]

    fun stopAll() {
        val paths = watchers.keys().toList()
        for (path in paths) {
            val watcher = watchers.remove(path) ?: continue
            try {
                watcher.stop()
                watcher.close()
            } catch (_: Exception) {}
        }
    }

    fun stopForModule(moduleId: String) {
        // Watchers are stored by path; clean up those whose path starts with the moduleId prefix
        // Actual path-to-module mapping depends on convention; here we do a best-effort prefix match
        val toRemove = watchers.keys.filter { it.startsWith("$moduleId/") || it.startsWith("$moduleId\\") }
        for (path in toRemove) {
            val watcher = watchers.remove(path) ?: continue
            try {
                watcher.stop()
                watcher.close()
            } catch (_: Exception) {}
        }
    }

    fun size(): Int = watchers.size
}
