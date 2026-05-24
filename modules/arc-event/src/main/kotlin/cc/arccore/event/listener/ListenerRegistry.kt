package cc.arccore.event.listener

import cc.arccore.api.module.ModuleContainerView
import org.bukkit.event.Listener

interface ListenerRegistry {
    fun register(owner: ModuleContainerView, listener: Listener): ListenerMetadata
    fun unregister(listener: Listener)
    fun unregisterAll(owner: ModuleContainerView): Int
    fun getListeners(owner: ModuleContainerView): List<ListenerMetadata>
    fun isRegistered(listener: Listener): Boolean
    fun registeredCount(): Int
}
