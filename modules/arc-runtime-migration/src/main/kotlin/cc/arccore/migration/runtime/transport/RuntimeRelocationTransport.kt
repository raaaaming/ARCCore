package cc.arccore.migration.runtime.transport

interface RuntimeRelocationTransport {
    val transportId: String

    fun canReach(targetNodeId: String): Boolean

    fun ping(targetNodeId: String): TransportPingResult

    fun transferSnapshot(
        targetNodeId: String,
        snapshotData: ByteArray,
        moduleId: String,
        snapshotId: String
    ): TransportTransferResult

    fun triggerBootstrap(
        targetNodeId: String,
        moduleId: String,
        snapshotId: String
    ): TransportBootstrapResult

    fun awaitRestoreReady(
        targetNodeId: String,
        moduleId: String,
        timeoutMs: Long
    ): TransportReadinessResult

    fun notifyRoutingSwitch(targetNodeId: String, moduleId: String): TransportAckResult

    fun supportsStreaming(): Boolean = false

    fun supportsMulticast(): Boolean = false
}
