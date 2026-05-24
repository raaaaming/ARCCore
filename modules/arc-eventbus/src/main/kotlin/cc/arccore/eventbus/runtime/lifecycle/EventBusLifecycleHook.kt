package cc.arccore.eventbus.runtime.lifecycle

/**
 * Callback interface for [InternalEventBus] lifecycle events.
 *
 * Implementations can be registered to receive notifications when the bus
 * starts up or shuts down.
 */
interface EventBusLifecycleHook {

    /**
     * Called immediately before the event bus begins accepting subscriptions.
     * Default implementation does nothing.
     */
    fun onStartup() {}

    /**
     * Called when [InternalEventBus.shutdown] is invoked.
     *
     * At this point the bus is still alive — subscriptions have not yet been cancelled.
     * Use this hook to perform pre-shutdown cleanup (e.g., flush async queues).
     */
    fun onShutdown() {}

    /**
     * Called after all subscriptions have been cancelled and the bus is fully stopped.
     * Default implementation does nothing.
     */
    fun onShutdownComplete() {}
}
