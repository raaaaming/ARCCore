package cc.arccore.event.bridge

import cc.arccore.api.module.ModuleContainerView
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class BukkitEventBridge(
    private val plugin: JavaPlugin
) : EventBridge {

    override fun register(listener: Listener, owner: ModuleContainerView) {
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }

    override fun unregister(listener: Listener) {
        HandlerList.unregisterAll(listener)
    }
}
