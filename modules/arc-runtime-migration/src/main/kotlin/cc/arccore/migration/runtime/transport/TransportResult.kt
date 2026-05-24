package cc.arccore.migration.runtime.transport

sealed class TransportPingResult {
    data class Reachable(val latencyMs: Long) : TransportPingResult()
    data class Unreachable(val reason: String) : TransportPingResult()
}

sealed class TransportTransferResult {
    data class Success(val transferredBytes: Long, val durationMs: Long) : TransportTransferResult()
    data class Failure(val error: Throwable) : TransportTransferResult()
}

sealed class TransportBootstrapResult {
    data object Success : TransportBootstrapResult()
    data class Failure(val error: Throwable) : TransportBootstrapResult()
}

sealed class TransportReadinessResult {
    data object Ready : TransportReadinessResult()
    data class NotReady(val reason: String) : TransportReadinessResult()
    data object TimedOut : TransportReadinessResult()
}

sealed class TransportAckResult {
    data object Acknowledged : TransportAckResult()
    data class Failed(val error: Throwable) : TransportAckResult()
}
