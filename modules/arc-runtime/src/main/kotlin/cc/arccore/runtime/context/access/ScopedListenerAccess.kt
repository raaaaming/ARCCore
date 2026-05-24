package cc.arccore.runtime.context.access

import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

interface ScopedListenerAccess {
    fun register(listener: Listener, plugin: Plugin, autoCleanup: Boolean = true)
    fun unregister(listener: Listener)
    fun unregisterAll(): Int
    fun registeredListeners(): Set<Listener>
}
