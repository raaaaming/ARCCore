package cc.arccore.zerodowntime.runtime.model

import java.util.concurrent.ConcurrentHashMap

internal class MutableOwnershipTransferStats {
    var schedulerTasksTransferred: Int = 0
    var serviceBindingsTransferred: Int = 0
    var eventSubscriptionsTransferred: Int = 0
    var commandsTransferred: Int = 0
    var coroutineScopesReplaced: Int = 0
    var transferDurationMs: Long = 0L

    fun toImmutable() = OwnershipTransferStats(
        schedulerTasksTransferred,
        serviceBindingsTransferred,
        eventSubscriptionsTransferred,
        commandsTransferred,
        coroutineScopesReplaced,
        transferDurationMs
    )
}

internal class TransitionContext(
    val targetModuleId: String,
    val affectedModuleIds: List<String> = emptyList(),
    val oldHandle: RuntimeHandle
) {
    val startTimeMs: Long = System.currentTimeMillis()

    @Volatile var phase: ZeroDowntimePhase = ZeroDowntimePhase.IDLE
    @Volatile var newHandle: RuntimeHandle? = null

    val capturedStates: MutableMap<String, Map<String, Any?>> = ConcurrentHashMap()
    val ownershipTransferStats: MutableOwnershipTransferStats = MutableOwnershipTransferStats()
    val drainRecord: DrainRecord = DrainRecord()

    @Volatile var rollbackAvailable: Boolean = true
    @Volatile var rollbackSnapshot: Any? = null

    val elapsedMs: Long get() = System.currentTimeMillis() - startTimeMs
}
