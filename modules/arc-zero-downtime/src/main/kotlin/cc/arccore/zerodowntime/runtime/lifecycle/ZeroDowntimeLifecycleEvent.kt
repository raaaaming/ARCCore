package cc.arccore.zerodowntime.runtime.lifecycle

import cc.arccore.zerodowntime.runtime.model.OwnershipTransferStats
import cc.arccore.zerodowntime.runtime.model.ZeroDowntimePhase
import cc.arccore.zerodowntime.runtime.rollback.ZeroDowntimeRollbackResult
import java.time.Instant

sealed class ZeroDowntimeLifecycleEvent {
    abstract val moduleId: String
    abstract val timestamp: Instant

    data class TransitionStarted(
        override val moduleId: String,
        val oldGeneration: Int,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class NewRuntimeBootstrapped(
        override val moduleId: String,
        val newGeneration: Int,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class OwnershipTransferCompleted(
        override val moduleId: String,
        val stats: OwnershipTransferStats,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class RoutingSwitched(
        override val moduleId: String,
        val durationMs: Long,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class OldRuntimeCleaned(
        override val moduleId: String,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class TransitionCompleted(
        override val moduleId: String,
        val totalDurationMs: Long,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class TransitionFailed(
        override val moduleId: String,
        val phase: ZeroDowntimePhase,
        val error: Throwable,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()

    data class TransitionRolledBack(
        override val moduleId: String,
        val rollbackResult: ZeroDowntimeRollbackResult,
        override val timestamp: Instant = Instant.now()
    ) : ZeroDowntimeLifecycleEvent()
}
