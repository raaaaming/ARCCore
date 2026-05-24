package cc.arccore.diagnostics.exception

open class DiagnosticsException(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause)

class SnapshotCreationException(reason: String, cause: Throwable? = null)
    : DiagnosticsException("Failed to create runtime snapshot: $reason", cause)

class InvalidRuntimeStateException(val moduleId: String, reason: String)
    : DiagnosticsException("Invalid runtime state for module '$moduleId': $reason")
