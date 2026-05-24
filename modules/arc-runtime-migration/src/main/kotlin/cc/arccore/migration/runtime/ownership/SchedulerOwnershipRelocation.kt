package cc.arccore.migration.runtime.ownership

import cc.arccore.migration.runtime.model.MigrationContext
import java.util.concurrent.ConcurrentHashMap

internal class SchedulerOwnershipRelocation {
    private val releasedTasks = ConcurrentHashMap<String, MutableList<String>>()

    fun releaseFromSource(moduleId: String, context: MigrationContext): Int {
        val count = 0
        releasedTasks.getOrPut(moduleId) { mutableListOf() }
        return count
    }

    fun assignToTarget(moduleId: String, targetNodeId: String, context: MigrationContext): Int {
        val released = releasedTasks.remove(moduleId)?.size ?: 0
        return released
    }

    fun rollback(moduleId: String, context: MigrationContext): Boolean {
        releasedTasks.remove(moduleId)
        return true
    }
}
