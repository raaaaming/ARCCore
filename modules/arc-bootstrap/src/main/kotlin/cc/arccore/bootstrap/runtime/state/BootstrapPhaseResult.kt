package cc.arccore.bootstrap.runtime.state

import cc.arccore.bootstrap.runtime.BootstrapPhase

sealed class BootstrapPhaseResult {

    abstract val phase: BootstrapPhase
    abstract val durationNanos: Long

    data class Success(
        override val phase: BootstrapPhase,
        override val durationNanos: Long,
        val notes: List<String> = emptyList()
    ) : BootstrapPhaseResult()

    data class Failure(
        override val phase: BootstrapPhase,
        override val durationNanos: Long,
        val cause: Throwable,
        val notes: List<String> = emptyList()
    ) : BootstrapPhaseResult()

    data class Skipped(
        override val phase: BootstrapPhase,
        override val durationNanos: Long = 0L,
        val reason: String = ""
    ) : BootstrapPhaseResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isSkipped: Boolean get() = this is Skipped
}
