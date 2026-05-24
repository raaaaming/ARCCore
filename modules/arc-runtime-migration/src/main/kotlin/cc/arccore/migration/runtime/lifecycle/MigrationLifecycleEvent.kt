package cc.arccore.migration.runtime.lifecycle

import cc.arccore.migration.runtime.model.MigrationId
import java.time.Instant

sealed class MigrationLifecycleEvent {
    abstract val migrationId: MigrationId
    abstract val moduleId: String
    abstract val timestamp: Instant

    data class MigrationStarted(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val sourceNodeId: String,
        val targetNodeId: String,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class DrainCompleted(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val drainDurationMs: Long,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class SnapshotCaptured(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val snapshotSizeBytes: Long,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class OwnershipReleased(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val releasedTasks: Int,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class TargetBootstrapped(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val targetNodeId: String,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class StateRestored(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val targetNodeId: String,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class RoutingSwitched(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val newGeneration: Int,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class OwnershipAssigned(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val assignedTasks: Int,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class MigrationCompleted(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val totalDurationMs: Long,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class MigrationFailed(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val error: Throwable,
        val phase: cc.arccore.migration.runtime.model.MigrationPhase,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class MigrationRolledBack(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val phase: cc.arccore.migration.runtime.model.MigrationPhase,
        val rollbackSuccess: Boolean,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()

    data class MigrationAborted(
        override val migrationId: MigrationId,
        override val moduleId: String,
        val phase: cc.arccore.migration.runtime.model.MigrationPhase,
        val reason: String,
        override val timestamp: Instant = Instant.now()
    ) : MigrationLifecycleEvent()
}
