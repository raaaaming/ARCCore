package cc.arccore.bootstrap.runtime.exception

import cc.arccore.bootstrap.runtime.BootstrapPhase

class BootstrapPhaseException(
    val phase: BootstrapPhase,
    val moduleId: String,
    message: String,
    cause: Throwable? = null
) : BootstrapException(
    "Bootstrap failed at phase $phase for module '$moduleId': $message",
    cause
)
