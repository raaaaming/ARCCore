package cc.arccore.runtime.context.access

import cc.arccore.api.module.CleanupScope
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class DefaultScopedListenerAccess(
    private val cleanupScope: CleanupScope
) : ScopedListenerAccess {

    private val listeners: MutableSet<Listener> = Collections.synchronizedSet(mutableSetOf())
    private val cleanupRegistered = AtomicBoolean(false)

    override fun register(listener: Listener, plugin: Plugin, autoCleanup: Boolean) {
        plugin.server.pluginManager.registerEvents(listener, plugin)
        listeners.add(listener)
        if (autoCleanup && cleanupRegistered.compareAndSet(false, true)) {
            cleanupScope.onClose { unregisterAll() }
        }
    }

    override fun unregister(listener: Listener) {
        HandlerList.unregisterAll(listener)
        listeners.remove(listener)
    }

    override fun unregisterAll(): Int {
        val count = listeners.size
        val snapshot = listeners.toSet()
        for (listener in snapshot) {
            HandlerList.unregisterAll(listener)
        }
        listeners.clear()
        return count
    }

    override fun registeredListeners(): Set<Listener> = listeners.toSet()
}
