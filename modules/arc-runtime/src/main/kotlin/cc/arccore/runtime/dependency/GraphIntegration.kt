package cc.arccore.runtime.dependency

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.runtime.lifecycle.CircularDependencyException
import cc.arccore.runtime.lifecycle.LifecycleOrchestrator

class GraphIntegration(
    private val validator: DependencyGraphValidator = DependencyGraphValidator(),
    private val orchestrator: LifecycleOrchestrator
) {

    fun validateAndSort(containers: Collection<ModuleContainerView>): List<ModuleContainerView> {
        val result = validateOnly(containers)
        return when (result) {
            is GraphValidationResult.Valid -> result.sortedForEnable
            is GraphValidationResult.Invalid -> {
                val errors = result.violations.filter { it.severity == DependencyViolation.Severity.ERROR }
                throw GraphValidationException(errors)
            }
        }
    }

    fun validateOnly(containers: Collection<ModuleContainerView>): GraphValidationResult =
        validator.validate(containers)

    fun sortForDisable(containers: Collection<ModuleContainerView>): List<ModuleContainerView> {
        val result = validateOnly(containers)
        return when (result) {
            is GraphValidationResult.Valid -> result.sortedForDisable
            is GraphValidationResult.Invalid -> {
                try {
                    orchestrator.sortForDisable(containers)
                } catch (e: CircularDependencyException) {
                    containers.toList()
                }
            }
        }
    }
}

class GraphValidationException(
    val violations: List<DependencyViolation>,
    message: String = buildMessage(violations)
) : RuntimeException(message) {
    companion object {
        fun buildMessage(violations: List<DependencyViolation>): String {
            val counts = violations.groupBy { it::class.simpleName }
                .mapValues { it.value.size }
            val summary = counts.entries.joinToString(", ") { "${it.value} ${it.key}" }
            return "Dependency graph validation failed: $summary"
        }
    }
}
