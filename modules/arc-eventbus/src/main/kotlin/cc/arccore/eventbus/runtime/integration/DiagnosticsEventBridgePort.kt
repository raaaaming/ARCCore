package cc.arccore.eventbus.runtime.integration

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent

/**
 * Port interface for bridging event bus diagnostics to an external diagnostics system
 * (e.g., arc-diagnostics module metrics).
 *
 * arc-eventbus does not depend on arc-diagnostics at compile time.
 * Instead, arc-diagnostics (or any consumer) can implement this port and inject it
 * into [DefaultInternalEventBus] to receive dispatch telemetry.
 *
 * Default no-op: [NoopDiagnosticsEventBridgePort]
 */
interface DiagnosticsEventBridgePort {

    /**
     * Called after each event dispatch completes.
     *
     * @param envelope The dispatched event envelope.
     * @param result The dispatch result.
     */
    fun <T : InternalEvent> onDispatched(
        envelope: EventEnvelope<T>,
        result: DispatchResult
    )

    /**
     * Called when the event bus shuts down.
     */
    fun onShutdown()
}
