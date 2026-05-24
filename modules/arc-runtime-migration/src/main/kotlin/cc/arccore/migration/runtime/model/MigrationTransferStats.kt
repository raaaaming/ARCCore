package cc.arccore.migration.runtime.model

data class MigrationTransferStats(
    val schedulerTasksRelocated: Int,
    val eventSubscriptionsRelocated: Int,
    val commandsRelocated: Int,
    val snapshotTransferDurationMs: Long,
    val ownershipTransferDurationMs: Long
)

internal class MutableMigrationTransferStats {
    var schedulerTasksRelocated: Int = 0
    var eventSubscriptionsRelocated: Int = 0
    var commandsRelocated: Int = 0
    var snapshotTransferDurationMs: Long = 0L
    var ownershipTransferDurationMs: Long = 0L

    fun toImmutable(): MigrationTransferStats = MigrationTransferStats(
        schedulerTasksRelocated = schedulerTasksRelocated,
        eventSubscriptionsRelocated = eventSubscriptionsRelocated,
        commandsRelocated = commandsRelocated,
        snapshotTransferDurationMs = snapshotTransferDurationMs,
        ownershipTransferDurationMs = ownershipTransferDurationMs
    )
}
