package cc.arccore.zerodowntime.runtime.hotswap

sealed class ReadinessResult {
    data object Ready : ReadinessResult()
    data class NotReady(val reason: String, val retryAfterMs: Long = 500L) : ReadinessResult()
    data class Failed(val cause: Throwable) : ReadinessResult()
}

interface ZeroDowntimeReadinessProbe {
    fun isReady(): ReadinessResult
    val readinessTimeoutMs: Long get() = 5000L
}
