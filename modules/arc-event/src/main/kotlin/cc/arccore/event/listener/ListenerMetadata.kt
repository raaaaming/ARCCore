package cc.arccore.event.listener

import cc.arccore.api.module.ModuleContainerView
import org.bukkit.event.Listener
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

data class ListenerMetadata(
    val registrationId: UUID,
    val owner: ModuleContainerView,
    val listener: Listener,
    val listenerClass: KClass<out Listener>,
    val listenerClassLoader: ClassLoader,
    val registeredAt: Instant
)
