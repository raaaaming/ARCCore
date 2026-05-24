package cc.arccore.zerodowntime.runtime.model

sealed class ZeroDowntimeReloadResult {
    data class Success(
        val moduleId: String,
        val transitionDurationMs: Long,
        val drainDurationMs: Long,
        val ownershipTransferStats: OwnershipTransferStats = OwnershipTransferStats.EMPTY,
        val affectedModules: List<String> = emptyList()
    ) : ZeroDowntimeReloadResult()

    data class Failure(
        val moduleId: String,
        val phase: ZeroDowntimePhase,
        val error: Throwable,
        val rollbackSuccess: Boolean = false,
        val partialOutcomes: List<String> = emptyList()
    ) : ZeroDowntimeReloadResult()

    data class Rejected(
        val moduleId: String,
        val reason: String
    ) : ZeroDowntimeReloadResult()

    data class AlreadyTransitioning(
        val moduleId: String
    ) : ZeroDowntimeReloadResult()
}
