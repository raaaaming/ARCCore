package cc.arccore.config.runtime.ownership

import cc.arccore.config.runtime.ConfigHandle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry tracking which module owns which [ConfigHandle] instances.
 *
 * Uses [ConcurrentHashMap] + [CopyOnWriteArrayList] so concurrent registrations
 * from multiple threads are safe. [computeIfAbsent] is used for atomic list creation.
 */
class DefaultConfigOwnershipRegistry : ConfigOwnershipRegistry {

    private val registry: ConcurrentHashMap<String, CopyOnWriteArrayList<ConfigHandle<*>>> =
        ConcurrentHashMap()

    override fun register(moduleId: String, handle: ConfigHandle<*>) {
        registry.computeIfAbsent(moduleId) { CopyOnWriteArrayList() }.add(handle)
    }

    override fun closeAll(moduleId: String): Int {
        val handles = registry.remove(moduleId) ?: return 0
        var count = 0
        for (handle in handles) {
            try {
                handle.close()
                count++
            } catch (_: Exception) {
                // Best-effort close; individual failures must not block the rest
            }
        }
        return count
    }

    override fun closeAllHandles(): Int {
        val keys = registry.keys().toList()
        var total = 0
        for (key in keys) {
            total += closeAll(key)
        }
        return total
    }

    override fun registeredModules(): Set<String> = registry.keys.toSet()
}
