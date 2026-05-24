package cc.arccore.bootstrap.runtime.exception

import cc.arccore.bootstrap.runtime.BootstrapPhase

class BootstrapValidationException(
    val phase: BootstrapPhase,
    val reason: String,
    cause: Throwable? = null
) : BootstrapException(
    "Bootstrap validation failed at $phase: $reason",
    cause
)
