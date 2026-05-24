package cc.arccore.event.listener

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.event.bridge.EventBridge
import cc.arccore.event.validation.ListenerValidator
import org.bukkit.event.Listener
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DefaultListenerRegistry(
    private val bridge: EventBridge,
    private val validator: ListenerValidator = ListenerValidator()
) : ListenerRegistry {

    private val byOwner = ConcurrentHashMap<String, CopyOnWriteArrayList<ListenerMetadata>>()
    private val byListener = ConcurrentHashMap<Listener, ListenerMetadata>()

    override fun register(owner: ModuleContainerView, listener: Listener): ListenerMetadata {
        validator.validate(owner, listener, byListener)
        val metadata = ListenerMetadata(
            registrationId = UUID.randomUUID(),
            owner = owner,
            listener = listener,
            listenerClass = listener::class,
            listenerClassLoader = listener::class.java.classLoader,
            registeredAt = Instant.now()
        )
        byOwner.getOrPut(owner.module.id) { CopyOnWriteArrayList() }.add(metadata)
        byListener[listener] = metadata
        bridge.register(listener, owner)
        return metadata
    }

    override fun unregister(listener: Listener) {
        val metadata = byListener.remove(listener) ?: return
        bridge.unregister(listener)
        byOwner[metadata.owner.module.id]?.remove(metadata)
    }

    override fun unregisterAll(owner: ModuleContainerView): Int {
        val listeners = byOwner.remove(owner.module.id) ?: return 0
        listeners.forEach { metadata ->
            bridge.unregister(metadata.listener)
            byListener.remove(metadata.listener)
        }
        return listeners.size
    }

    override fun getListeners(owner: ModuleContainerView): List<ListenerMetadata> =
        byOwner[owner.module.id]?.toList() ?: emptyList()

    override fun isRegistered(listener: Listener): Boolean = byListener.containsKey(listener)

    override fun registeredCount(): Int = byListener.size
}
