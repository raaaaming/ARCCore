package cc.arccore.eventbus.runtime.event

/**
 * Marker interface for all events dispatched through [InternalEventBus].
 *
 * All framework-internal and module-to-module events must implement this interface.
 * Bukkit/Paper events must NOT extend this — use the standard Bukkit event system instead.
 */
interface InternalEvent {

    /**
     * A human-readable name for this event, used for diagnostics and logging.
     * Defaults to the simple class name.
     */
    val eventName: String get() = this::class.simpleName ?: "UnknownEvent"
}
