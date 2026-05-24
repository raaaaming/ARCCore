package cc.arccore.runtime.dependency

import cc.arccore.api.module.ModuleContainerView

class DependencyGraphValidator {

    private enum class Color { WHITE, GRAY, BLACK }

    fun validate(containers: Collection<ModuleContainerView>): GraphValidationResult {
        val graph = DependencyGraph.build(containers)
        return validateGraph(graph)
    }

    fun validateGraph(graph: DependencyGraph): GraphValidationResult {
        val violations = mutableListOf<DependencyViolation>()

        violations += checkMissingDependencies(graph)
        violations += checkVersionMismatches(graph)
        violations += checkMissingSoftDependencies(graph)
        violations += detectCycles(graph)

        val errorViolations = violations.filter { it.severity == DependencyViolation.Severity.ERROR }

        val warningViolations = violations.filter { it.severity == DependencyViolation.Severity.WARNING }

        return if (errorViolations.isEmpty()) {
            val sorted = topologicalSort(graph)
            GraphValidationResult.Valid(graph, sorted, warningViolations)
        } else {
            GraphValidationResult.Invalid(graph, violations)
        }
    }

    internal fun checkMissingDependencies(graph: DependencyGraph): List<DependencyViolation.MissingDependency> {
        val violations = mutableListOf<DependencyViolation.MissingDependency>()

        graph.nodes.forEach { (id, container) ->
            container.description.dependencies
                .filter { !it.optional && it.id !in graph.nodes }
                .forEach { dep ->
                    violations += DependencyViolation.MissingDependency(
                        dependentId = id,
                        missingId = dep.id
                    )
                }
        }

        return violations
    }

    internal fun checkVersionMismatches(graph: DependencyGraph): List<DependencyViolation.VersionMismatch> {
        val violations = mutableListOf<DependencyViolation.VersionMismatch>()

        graph.nodes.forEach { (id, container) ->
            val allDeps = container.description.dependencies + container.description.softDependencies

            allDeps.forEach { dep ->
                val versionRange = dep.versionRange ?: return@forEach
                val targetContainer = graph.nodes[dep.id] ?: return@forEach

                if (!versionRange.satisfiedBy(targetContainer.description.version)) {
                    violations += DependencyViolation.VersionMismatch(
                        dependentId = id,
                        dependencyId = dep.id,
                        required = versionRange,
                        actual = targetContainer.description.version
                    )
                }
            }
        }

        return violations
    }

    internal fun checkMissingSoftDependencies(graph: DependencyGraph): List<DependencyViolation.MissingSoftDependency> {
        val violations = mutableListOf<DependencyViolation.MissingSoftDependency>()

        graph.nodes.forEach { (id, container) ->
            container.description.softDependencies
                .filter { it.id !in graph.nodes }
                .forEach { dep ->
                    violations += DependencyViolation.MissingSoftDependency(
                        dependentId = id,
                        missingId = dep.id
                    )
                }
        }

        return violations
    }

    internal fun detectCycles(graph: DependencyGraph): List<DependencyViolation.CircularDependency> {
        val color = mutableMapOf<String, Color>()
        graph.allModuleIds().forEach { color[it] = Color.WHITE }

        val foundCycles = mutableSetOf<String>()
        val violations = mutableListOf<DependencyViolation.CircularDependency>()
        val path = mutableListOf<String>()

        fun dfs(nodeId: String) {
            color[nodeId] = Color.GRAY
            path.add(nodeId)

            for (neighbor in graph.dependenciesOf(nodeId)) {
                when (color[neighbor]) {
                    Color.GRAY -> {
                        val cycleStart = path.indexOf(neighbor)
                        val cycle = path.subList(cycleStart, path.size).toList()
                        if (cycle.isEmpty()) return@dfs

                        val minId = cycle.minOrNull() ?: return@dfs
                        val minIndex = cycle.indexOf(minId)
                        val normalized = cycle.subList(minIndex, cycle.size) + cycle.subList(0, minIndex)
                        val key = normalized.joinToString(",")

                        if (foundCycles.add(key)) {
                            violations += DependencyViolation.CircularDependency(normalized)
                        }
                    }
                    Color.WHITE -> {
                        if (neighbor in graph.nodes) dfs(neighbor)
                    }
                    Color.BLACK -> {}
                    null -> {}
                }
            }

            path.removeAt(path.size - 1)
            color[nodeId] = Color.BLACK
        }

        graph.allModuleIds().sorted().forEach { id ->
            if (color[id] == Color.WHITE) dfs(id)
        }

        return violations
    }

    internal fun topologicalSort(graph: DependencyGraph): List<ModuleContainerView> {
        val inDegree = graph.allModuleIds().associateWith { graph.dependenciesOf(it).size }.toMutableMap()

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.keys.sorted().forEach { queue.add(it) }

        val sorted = mutableListOf<ModuleContainerView>()

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            val container = graph.containerOf(id) ?: continue
            sorted.add(container)

            graph.reverseEdges[id]?.sorted()?.forEach { dependentId ->
                inDegree[dependentId] = (inDegree[dependentId] ?: 0) - 1
                if (inDegree[dependentId] == 0) queue.add(dependentId)
            }
        }

        if (sorted.size != graph.nodes.size) {
            throw IllegalStateException(
                "Topological sort failed: cycle detected among ${graph.nodes.size} nodes, sorted ${sorted.size}"
            )
        }

        return sorted
    }
}
