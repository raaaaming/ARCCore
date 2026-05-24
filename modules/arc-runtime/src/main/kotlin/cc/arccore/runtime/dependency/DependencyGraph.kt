package cc.arccore.runtime.dependency

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleDependency

class DependencyGraph private constructor(
    val nodes: Map<String, ModuleContainerView>,
    val edges: Map<String, Set<String>>,
    val reverseEdges: Map<String, Set<String>>,
    /** 하드 의존성(softDependencies 제외)에 의한 역방향 엣지만 포함 */
    private val hardReverseEdges: Map<String, Set<String>>
) {

    fun dependenciesOf(moduleId: String): Set<String> = edges[moduleId] ?: emptySet()

    /**
     * @param hardOnly true이면 하드 의존성 역방향만 반환 (소프트 의존성 제외)
     */
    fun dependentsOf(moduleId: String, hardOnly: Boolean = false): Set<String> =
        if (hardOnly) hardReverseEdges[moduleId] ?: emptySet()
        else reverseEdges[moduleId] ?: emptySet()

    fun allModuleIds(): Set<String> = nodes.keys

    fun containerOf(moduleId: String): ModuleContainerView? = nodes[moduleId]

    fun declaredDependenciesOf(moduleId: String): List<ModuleDependency> {
        val container = nodes[moduleId] ?: return emptyList()
        return container.description.dependencies + container.description.softDependencies
    }

    companion object {

        fun build(containers: Collection<ModuleContainerView>): DependencyGraph {
            val nodes = containers.associateBy { it.module.id }

            val edges = mutableMapOf<String, Set<String>>()
            val reverseEdges = mutableMapOf<String, MutableSet<String>>()
            val hardReverseEdges = mutableMapOf<String, MutableSet<String>>()

            nodes.keys.forEach { id ->
                edges[id] = emptySet()
                reverseEdges[id] = mutableSetOf()
                hardReverseEdges[id] = mutableSetOf()
            }

            nodes.forEach { (id, container) ->
                val allDeps = (container.description.dependencies + container.description.softDependencies)
                    .filter { it.id in nodes }
                    .map { it.id }
                    .distinct()
                    .toSet()

                edges[id] = allDeps

                allDeps.forEach { depId ->
                    reverseEdges.getOrPut(depId) { mutableSetOf() }.add(id)
                }

                // 하드 의존성만 별도 역방향 엣지 추적
                container.description.dependencies
                    .filter { it.id in nodes }
                    .forEach { dep ->
                        hardReverseEdges.getOrPut(dep.id) { mutableSetOf() }.add(id)
                    }
            }

            return DependencyGraph(
                nodes = nodes,
                edges = edges,
                reverseEdges = reverseEdges.mapValues { it.value.toSet() },
                hardReverseEdges = hardReverseEdges.mapValues { it.value.toSet() }
            )
        }

        fun buildSubgraph(
            containers: Collection<ModuleContainerView>,
            includeIds: Set<String>
        ): DependencyGraph {
            val filtered = containers.filter { it.module.id in includeIds }
            return build(filtered)
        }
    }
}
