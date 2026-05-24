package cc.arccore.storage.runtime.integration

import java.util.UUID

/**
 * No-op implementation of [DiagnosticsStorageBridgePort].
 *
 * Used as the default when arc-diagnostics is not present or not yet initialised.
 */
object NoopDiagnosticsStorageBridgePort : DiagnosticsStorageBridgePort {
    override fun onHandleOpened(moduleId: String, handleId: UUID, storageType: String) = Unit
    override fun onHandleClosed(moduleId: String, handleId: UUID) = Unit
}
