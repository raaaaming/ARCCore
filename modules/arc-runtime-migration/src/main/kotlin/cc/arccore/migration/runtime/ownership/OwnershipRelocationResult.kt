package cc.arccore.migration.runtime.ownership

data class OwnershipReleaseResult(
    val releasedSchedulerTasks: Int,
    val releasedEventSubscriptions: Int,
    val releasedCommands: Int,
    val durationMs: Long
)

data class OwnershipAssignResult(
    val assignedSchedulerTasks: Int,
    val assignedEventSubscriptions: Int,
    val assignedCommands: Int,
    val durationMs: Long
)
