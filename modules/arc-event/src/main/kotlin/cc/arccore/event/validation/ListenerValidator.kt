package cc.arccore.event.validation

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.event.exception.DuplicateListenerException
import cc.arccore.event.exception.InvalidListenerException
import cc.arccore.event.listener.ListenerMetadata
import org.bukkit.event.Listener

class ListenerValidator {

    fun validate(
        owner: ModuleContainerView,
        listener: Listener,
        registered: Map<Listener, ListenerMetadata>
    ) {
        if (owner.isTerminal()) {
            throw InvalidListenerException(
                "Module '${owner.module.id}' is terminated (state=${owner.state}). Cannot register listener."
            )
        }
        if (!owner.isActive()) {
            throw InvalidListenerException(
                "Module '${owner.module.id}' must be ENABLED to register listeners. Current state: ${owner.state}"
            )
        }
        if (listener in registered) {
            throw DuplicateListenerException(
                listenerClass = listener::class.qualifiedName ?: "Unknown",
                ownerId = owner.module.id
            )
        }
    }
}
