package cc.arccore.bootstrap.runtime.scheduling

import cc.arccore.bootstrap.runtime.BootstrapContext
import cc.arccore.bootstrap.runtime.exception.StartupOptimizationException

/**
 * Sorts BootstrapContexts into a dependency-respecting execution plan using Kahn's algorithm.
 * Detects dependency cycles and throws [StartupOptimizationException] if found.
 */
class BootstrapOrchestrator {

    /**
     * Produces a [BootstrapExecutionPlan] with topologically sorted parallel tiers.
     *
     * @param contexts all module contexts to schedule
     * @throws StartupOptimizationException if a dependency cycle is detected
     */
    fun sortForBootstrap(contexts: List<BootstrapContext>): BootstrapExecutionPlan {
        if (contexts.isEmpty()) {
            return BootstrapExecutionPlan(
                parallelTiers = emptyList(),
                totalModuleCount = 0,
                dependencyOrder = emptyList()
            )
        }

        val contextMap: Map<String, BootstrapContext> = contexts.associateBy { it.moduleId }
        val allIds: Set<String> = contextMap.keys

        // Build adjacency: moduleId -> set of dependencies (that are present in contexts)
        val deps: Map<String, Set<String>> = contexts.associate { ctx ->
            val knownDeps = ctx.description.dependencies
                .map { it.id }
                .filter { it in allIds }
                .toSet()
            ctx.moduleId to knownDeps
        }

        // Compute in-degree: number of dependencies each module has that are in the set
        val inDegreeFixed: MutableMap<String, Int> = allIds.associateWith { id ->
            deps[id]?.size ?: 0
        }.toMutableMap()

        // Kahn's BFS
        val queue: ArrayDeque<String> = ArrayDeque(
            allIds.filter { (inDegreeFixed[it] ?: 0) == 0 }
        )

        val tiers = mutableListOf<List<BootstrapContext>>()
        val visited = mutableSetOf<String>()
        val sortedOrder = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val tierIds = queue.toList()
            queue.clear()

            val tierContexts = tierIds.mapNotNull { contextMap[it] }
            tiers.add(tierContexts)
            visited.addAll(tierIds)
            sortedOrder.addAll(tierIds)

            tierIds.forEach { id ->
                // Find modules that depend on 'id' and decrement their in-degree
                val dependents = allIds.filter { candidate ->
                    deps[candidate]?.contains(id) == true
                }
                dependents.forEach { dep ->
                    val newDegree = (inDegreeFixed[dep] ?: 1) - 1
                    inDegreeFixed[dep] = newDegree
                    if (newDegree == 0 && dep !in visited) {
                        queue.add(dep)
                    }
                }
            }
        }

        if (visited.size != allIds.size) {
            val cycleNodes = allIds - visited
            throw StartupOptimizationException(
                "Dependency cycle detected among modules: $cycleNodes"
            )
        }

        return BootstrapExecutionPlan(
            parallelTiers = tiers,
            totalModuleCount = contexts.size,
            dependencyOrder = sortedOrder
        )
    }
}
