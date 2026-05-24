package cc.arccore.bootstrap.runtime.state

import cc.arccore.bootstrap.runtime.BootstrapPhase
import cc.arccore.bootstrap.runtime.profiling.BootstrapProfilingData

sealed class BootstrapResult {

    abstract val moduleId: String
    abstract val profilingData: BootstrapProfilingData?

    data class Success(
        override val moduleId: String,
        val completedPhases: List<BootstrapPhase>,
        override val profilingData: BootstrapProfilingData? = null
    ) : BootstrapResult()

    data class Failure(
        override val moduleId: String,
        val failedPhase: BootstrapPhase,
        val cause: Throwable,
        val completedPhases: List<BootstrapPhase>,
        override val profilingData: BootstrapProfilingData? = null
    ) : BootstrapResult()

    data class Skipped(
        override val moduleId: String,
        val reason: String,
        override val profilingData: BootstrapProfilingData? = null
    ) : BootstrapResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isSkipped: Boolean get() = this is Skipped
}
