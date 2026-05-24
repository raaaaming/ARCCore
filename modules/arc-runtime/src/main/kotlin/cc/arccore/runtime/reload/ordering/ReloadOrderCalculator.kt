package cc.arccore.runtime.reload.ordering

import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.dependency.DependencyGraph

object ReloadOrderCalculator {

    /**
     * [targetId]의 모든 transitive dependent를 포함한 비활성화 순서.
     * dependent는 target보다 먼저 비활성화되어야 한다.
     *
     * @return [dep2, dep1, targetId] 형태 (depth-first, dependents first)
     */
    fun calculateDisableOrder(
        targetId: String,
        graph: DependencyGraph,
        enabledOnly: Boolean = true
    ): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(id: String) {
            if (!visited.add(id)) return
            val dependents = graph.dependentsOf(id)
            for (dep in dependents) {
                val container = graph.containerOf(dep) ?: continue
                if (enabledOnly) {
                    val state = container.state
                    if (state != ModuleState.ENABLED && state != ModuleState.DISABLING) continue
                }
                visit(dep)
            }
            result.add(id)
        }

        val directDependents = graph.dependentsOf(targetId)
        for (dep in directDependents) {
            visit(dep)
        }
        if (!visited.contains(targetId)) {
            result.add(targetId)
        }

        return result
    }

    /**
     * 활성화 순서: targetId 먼저, 이후 disable 순서의 역순 (target 제외).
     */
    fun calculateEnableOrder(
        targetId: String,
        disableOrder: List<String>
    ): List<String> {
        return disableOrder.reversed()
    }
}
