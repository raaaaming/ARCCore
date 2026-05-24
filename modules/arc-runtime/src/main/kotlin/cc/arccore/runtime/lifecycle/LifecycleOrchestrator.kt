package cc.arccore.runtime.lifecycle

import cc.arccore.api.module.ModuleContainerView
import java.util.LinkedList

/**
 * 모듈 의존성 그래프를 토폴로지 정렬(Kahn's algorithm)하여
 * enable/disable 순서를 보장합니다.
 *
 * - enable 순서: 의존 대상이 먼저 (B에 의존하는 A는 B 다음에 enable)
 * - disable/unload 순서: enable의 역순
 */
class LifecycleOrchestrator {

    /**
     * enable 우선순위 순서로 정렬합니다. 의존 대상 모듈이 앞에 위치합니다.
     * @throws CircularDependencyException 순환 의존성 탐지 시
     */
    fun sortForEnable(containers: Collection<ModuleContainerView>): List<ModuleContainerView> =
        topologicalSort(containers)

    /**
     * disable/unload 순서 = enable의 역순
     */
    fun sortForDisable(containers: Collection<ModuleContainerView>): List<ModuleContainerView> =
        topologicalSort(containers).reversed()

    /**
     * targetId를 직접 또는 간접 의존하는 모든 활성 모듈을 반환합니다.
     * 실패 전파 대상 계산에 사용됩니다. optional 의존성은 포함하지 않습니다.
     */
    fun findDependents(
        targetId: String,
        allContainers: Collection<ModuleContainerView>
    ): List<ModuleContainerView> {
        val dependentsMap = buildDependentsMap(allContainers)
        val result = mutableListOf<ModuleContainerView>()
        val visited = mutableSetOf<String>()
        val queue = LinkedList<String>()

        dependentsMap[targetId]?.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val id = queue.poll()
            if (id in visited) continue
            visited.add(id)
            val container = allContainers.find { it.module.id == id } ?: continue
            result.add(container)
            dependentsMap[id]?.forEach { if (it !in visited) queue.add(it) }
        }

        return result
    }

    private fun topologicalSort(containers: Collection<ModuleContainerView>): List<ModuleContainerView> {
        val containerMap = containers.associateBy { it.module.id }
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        containers.forEach { container ->
            val id = container.module.id
            inDegree.putIfAbsent(id, 0)
            adjacency.putIfAbsent(id, mutableListOf())
        }

        containers.forEach { container ->
            val id = container.module.id
            // hard dependencies + softDependencies 모두 순서에 반영
            // (실패 전파 제외 여부는 buildDependentsMap에서 별도 처리)
            val hardDeps = (container.description.dependencies + container.description.softDependencies)
                .filter { it.id in containerMap }
                .map { it.id }
                .distinct()

            hardDeps.forEach { depId ->
                // depId가 먼저 enable되어야 id가 enable될 수 있음
                adjacency.getOrPut(depId) { mutableListOf() }.add(id)
                inDegree[id] = (inDegree[id] ?: 0) + 1
            }
        }

        val queue = LinkedList<String>()
        inDegree.filter { it.value == 0 }.keys.sorted().forEach { queue.add(it) }

        val sorted = mutableListOf<ModuleContainerView>()
        while (queue.isNotEmpty()) {
            val id = queue.poll()
            val container = containerMap[id] ?: continue
            sorted.add(container)
            adjacency[id]?.sorted()?.forEach { neighbor ->
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) queue.add(neighbor)
            }
        }

        if (sorted.size != containers.size) {
            val remaining = containers
                .map { it.module.id }
                .filter { id -> sorted.none { it.module.id == id } }
            throw CircularDependencyException(
                "Circular dependency detected among modules: $remaining"
            )
        }

        return sorted
    }

    private fun buildDependentsMap(
        containers: Collection<ModuleContainerView>
    ): Map<String, Set<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        containers.forEach { container ->
            container.description.dependencies
                .filter { !it.optional }
                .forEach { dep ->
                    map.getOrPut(dep.id) { mutableSetOf() }.add(container.module.id)
                }
        }
        return map
    }
}

class CircularDependencyException(message: String) : RuntimeException(message)
