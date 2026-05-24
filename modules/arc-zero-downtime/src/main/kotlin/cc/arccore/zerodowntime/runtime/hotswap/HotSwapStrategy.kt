package cc.arccore.zerodowntime.runtime.hotswap

enum class HotSwapStrategyType {
    ZERO_DOWNTIME,
    GRACEFUL_RESTART,
    STATELESS_SWAP,
    DEFERRED
}

interface HotSwapStrategy {
    val strategyType: HotSwapStrategyType
    fun estimatedDowntimeMs(): Long
}

object ZeroDowntimeHotSwapStrategy : HotSwapStrategy {
    override val strategyType = HotSwapStrategyType.ZERO_DOWNTIME
    override fun estimatedDowntimeMs() = 0L
}

object GracefulRestartHotSwapStrategy : HotSwapStrategy {
    override val strategyType = HotSwapStrategyType.GRACEFUL_RESTART
    override fun estimatedDowntimeMs() = 500L
}

object StatelessSwapHotSwapStrategy : HotSwapStrategy {
    override val strategyType = HotSwapStrategyType.STATELESS_SWAP
    override fun estimatedDowntimeMs() = 50L
}

object DeferredHotSwapStrategy : HotSwapStrategy {
    override val strategyType = HotSwapStrategyType.DEFERRED
    override fun estimatedDowntimeMs() = Long.MAX_VALUE
}
