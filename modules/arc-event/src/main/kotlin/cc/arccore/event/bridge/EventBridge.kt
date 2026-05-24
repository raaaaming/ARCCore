package cc.arccore.event.bridge

import cc.arccore.api.module.ModuleContainerView
import org.bukkit.event.Listener

interface EventBridge {
    fun register(listener: Listener, owner: ModuleContainerView)
    fun unregister(listener: Listener)
}
