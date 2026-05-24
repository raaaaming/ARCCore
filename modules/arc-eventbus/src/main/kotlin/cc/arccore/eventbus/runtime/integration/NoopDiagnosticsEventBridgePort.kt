package cc.arccore.eventbus.runtime.integration

import cc.arccore.eventbus.runtime.dispatch.DispatchResult
import cc.arccore.eventbus.runtime.event.EventEnvelope
import cc.arccore.eventbus.runtime.event.InternalEvent

/**
 * No-op implementation of [DiagnosticsEventBridgePort].
 *
 * Used as the default bridge when no external diagnostics system is connected.
 * All methods do nothing.
 */
class NoopDiagnosticsEventBridgePort : DiagnosticsEventBridgePort {

    override fun <T : InternalEvent> onDispatched(
        envelope: EventEnvelope<T>,
        result: DispatchResult
    ) {
        // no-op
    }

    override fun onShutdown() {
        // no-op
    }
}
