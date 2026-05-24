package cc.arccore.zerodowntime.runtime.ownership

sealed class SchedulerTransferResult {
    data class Success(val transferred: Int, val cancelled: Int) : SchedulerTransferResult()
    data class PartialSuccess(val transferred: Int, val failed: Int) : SchedulerTransferResult()
    data class Skipped(val reason: String) : SchedulerTransferResult()
}

sealed class ServiceTransferResult {
    data class Success(val transferred: Int) : ServiceTransferResult()
    data class PartialSuccess(val transferred: Int, val warnings: List<String>) : ServiceTransferResult()
    data class Skipped(val reason: String) : ServiceTransferResult()
}

sealed class SubscriptionTransferResult {
    data class Success(val transferred: Int) : SubscriptionTransferResult()
    data class Skipped(val reason: String) : SubscriptionTransferResult()
}

sealed class CommandTransferResult {
    data class Success(val transferred: Int) : CommandTransferResult()
    data class Skipped(val reason: String) : CommandTransferResult()
}
