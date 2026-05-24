package cc.arccore.zerodowntime.runtime.model

data class OwnershipTransferStats(
    val schedulerTasksTransferred: Int = 0,
    val serviceBindingsTransferred: Int = 0,
    val eventSubscriptionsTransferred: Int = 0,
    val commandsTransferred: Int = 0,
    val coroutineScopesReplaced: Int = 0,
    val transferDurationMs: Long = 0L
) {
    companion object {
        val EMPTY = OwnershipTransferStats()
    }
}
