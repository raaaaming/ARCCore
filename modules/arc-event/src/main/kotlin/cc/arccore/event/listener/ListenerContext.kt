package cc.arccore.event.listener

import cc.arccore.api.module.ModuleContainerView
import org.bukkit.event.Listener

class ListenerContext(
    val owner: ModuleContainerView,
    private val registry: ListenerRegistry
) {
    fun register(listener: Listener): ListenerMetadata = registry.register(owner, listener)
    fun unregisterAll(): Int = registry.unregisterAll(owner)
    fun isRegistered(listener: Listener): Boolean = registry.isRegistered(listener)
    fun registeredListeners(): List<ListenerMetadata> = registry.getListeners(owner)
}
