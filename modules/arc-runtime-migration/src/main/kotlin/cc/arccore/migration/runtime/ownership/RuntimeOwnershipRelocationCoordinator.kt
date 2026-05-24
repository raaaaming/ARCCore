package cc.arccore.migration.runtime.ownership

import cc.arccore.migration.runtime.model.MigrationContext

internal class RuntimeOwnershipRelocationCoordinator(
    private val schedulerRelocation: SchedulerOwnershipRelocation = SchedulerOwnershipRelocation(),
    private val eventRelocation: EventSubscriptionRelocation = EventSubscriptionRelocation(),
    private val commandRelocation: CommandOwnershipRelocation = CommandOwnershipRelocation()
) {
    fun releaseFromSource(context: MigrationContext): OwnershipReleaseResult {
        val startMs = System.currentTimeMillis()
        val schedulerCount = schedulerRelocation.releaseFromSource(context.moduleId, context)
        val eventCount = eventRelocation.releaseFromSource(context.moduleId, context)
        val commandCount = commandRelocation.releaseFromSource(context.moduleId, context)
        return OwnershipReleaseResult(
            releasedSchedulerTasks = schedulerCount,
            releasedEventSubscriptions = eventCount,
            releasedCommands = commandCount,
            durationMs = System.currentTimeMillis() - startMs
        )
    }

    fun assignToTarget(context: MigrationContext): OwnershipAssignResult {
        val startMs = System.currentTimeMillis()
        val schedulerCount = schedulerRelocation.assignToTarget(context.moduleId, context.targetNodeId, context)
        val eventCount = eventRelocation.assignToTarget(context.moduleId, context.targetNodeId, context)
        val commandCount = commandRelocation.assignToTarget(context.moduleId, context.targetNodeId, context)
        return OwnershipAssignResult(
            assignedSchedulerTasks = schedulerCount,
            assignedEventSubscriptions = eventCount,
            assignedCommands = commandCount,
            durationMs = System.currentTimeMillis() - startMs
        )
    }

    fun rollbackToSource(context: MigrationContext): Boolean {
        val schedulerOk = schedulerRelocation.rollback(context.moduleId, context)
        val eventOk = eventRelocation.rollback(context.moduleId, context)
        val commandOk = commandRelocation.rollback(context.moduleId, context)
        return schedulerOk && eventOk && commandOk
    }
}
