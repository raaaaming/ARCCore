package cc.arccore.migration.runtime.coordination

import cc.arccore.migration.runtime.model.MigrationContext
import java.util.concurrent.ConcurrentHashMap

internal class MigrationRoutingSwitch(
    private val generationCounter: MigrationGenerationCounter
) {
    private val routedToTarget = ConcurrentHashMap.newKeySet<String>()

    fun switchToTarget(context: MigrationContext): RoutingSwitchOutcome {
        routedToTarget.add(context.moduleId)
        generationCounter.increment(context.moduleId)
        return RoutingSwitchOutcome.Switched(generationCounter.current(context.moduleId))
    }

    fun rollbackToSource(moduleId: String, context: MigrationContext): Boolean {
        routedToTarget.remove(moduleId)
        return true
    }

    fun isRoutedToTarget(moduleId: String): Boolean = routedToTarget.contains(moduleId)

    sealed class RoutingSwitchOutcome {
        data class Switched(val newGeneration: Int) : RoutingSwitchOutcome()
        data class Failed(val error: Throwable) : RoutingSwitchOutcome()
    }
}
