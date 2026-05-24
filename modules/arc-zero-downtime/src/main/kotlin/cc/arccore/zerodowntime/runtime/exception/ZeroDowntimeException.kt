package cc.arccore.zerodowntime.runtime.exception

import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase

open class ZeroDowntimeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ZeroDowntimeReloadException(
    val moduleId: String,
    val phase: ZeroDowntimePhase,
    message: String,
    cause: Throwable? = null
) : ZeroDowntimeException(message, cause)

class RuntimeTransitionException(
    val moduleId: String,
    val phase: ZeroDowntimePhase,
    message: String,
    cause: Throwable? = null
) : ZeroDowntimeException(message, cause)

class OwnershipTransferException(
    val moduleId: String,
    val transferType: String,
    message: String,
    cause: Throwable? = null
) : ZeroDowntimeException(message, cause)

class RuntimeDrainException(
    val moduleId: String,
    val remainingTasks: Int,
    val elapsedMs: Long,
    message: String
) : ZeroDowntimeException(message)

class BootstrapFailedException(
    val moduleId: String,
    message: String,
    cause: Throwable? = null
) : ZeroDowntimeException(message, cause)

class RollbackFailedException(
    val moduleId: String,
    val originalError: Throwable,
    message: String,
    cause: Throwable? = null
) : ZeroDowntimeException(message, cause)

class StaleRuntimeException(
    val moduleId: String,
    val expectedGeneration: Int,
    val actualGeneration: Int
) : ZeroDowntimeException(
    "Stale runtime detected for module '$moduleId': expected generation $expectedGeneration but found $actualGeneration"
)
