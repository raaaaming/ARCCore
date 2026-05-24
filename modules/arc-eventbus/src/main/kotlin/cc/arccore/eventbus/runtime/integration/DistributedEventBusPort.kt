package cc.arccore.eventbus.runtime.integration

import cc.arccore.eventbus.runtime.event.InternalEvent

/**
 * Port interface for optional distributed event bus integration.
 *
 * Allows events to be forwarded to or received from external nodes
 * (e.g., a Redis pub/sub backend, message broker, or cluster node).
 *
 * arc-eventbus does not implement distributed behavior directly.
 * Consumers that need cross-node events implement this port and inject it
 * into [DefaultInternalEventBus].
 *
 * Default: no distributed port is configured.
 */
interface DistributedEventBusPort {

    /**
     * Called after a local event is dispatched.
     *
     * Implementations may forward this event to remote nodes if appropriate.
     *
     * @param event The event that was dispatched locally.
     * @param publisherModuleId The module that published the event.
     */
    fun <T : InternalEvent> onLocalDispatch(event: T, publisherModuleId: String?)

    /**
     * Returns true if remote events should also be forwarded to local subscribers.
     * When true, the implementation is responsible for calling back into the local
     * [InternalEventBus] when a remote event is received.
     */
    fun acceptsRemoteEvents(): Boolean

    /**
     * Called when the event bus shuts down.
     * Implementations should disconnect from remote brokers here.
     */
    fun onShutdown()
}
