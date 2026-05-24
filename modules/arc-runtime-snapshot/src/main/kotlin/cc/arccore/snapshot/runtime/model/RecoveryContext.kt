package cc.arccore.snapshot.runtime.model

import java.time.Instant

internal class RecoveryContext(
    val targetRuntimeId: String,
    val snapshot: RuntimeSnapshot
) {
    val startedAt: Instant = Instant.now()
    @Volatile var phase: RecoveryPhase = RecoveryPhase.IDLE
    @Volatile var rollbackAvailable: Boolean = true

    val elapsedMs: Long get() = Instant.now().toEpochMilli() - startedAt.toEpochMilli()

    val stageResults: MutableMap<RecoveryPhase, StageOutcome> = mutableMapOf()

    sealed class StageOutcome {
        data object Success : StageOutcome()
        data class Failure(val error: Throwable) : StageOutcome()
        data class Skipped(val reason: String) : StageOutcome()
    }
}
