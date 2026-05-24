package cc.arccore.runtime.dependency

import cc.arccore.api.module.ModuleContainerView

sealed class GraphValidationResult {

    abstract val graph: DependencyGraph

    data class Valid(
        override val graph: DependencyGraph,
        val sortedForEnable: List<ModuleContainerView>,
        /** ERROR 없이 통과했으나 발생한 WARNING 위반 목록 */
        val warnings: List<DependencyViolation> = emptyList()
    ) : GraphValidationResult() {
        val sortedForDisable: List<ModuleContainerView> = sortedForEnable.reversed()
    }

    data class Invalid(
        override val graph: DependencyGraph,
        val violations: List<DependencyViolation>
    ) : GraphValidationResult() {
        val hasCycles: Boolean get() = violations.any { it is DependencyViolation.CircularDependency }
        val hasMissingDependencies: Boolean get() = violations.any { it is DependencyViolation.MissingDependency }
        val hasVersionMismatches: Boolean get() = violations.any { it is DependencyViolation.VersionMismatch }

        fun cycleViolations(): List<DependencyViolation.CircularDependency> =
            violations.filterIsInstance<DependencyViolation.CircularDependency>()

        fun missingViolations(): List<DependencyViolation.MissingDependency> =
            violations.filterIsInstance<DependencyViolation.MissingDependency>()

        fun versionViolations(): List<DependencyViolation.VersionMismatch> =
            violations.filterIsInstance<DependencyViolation.VersionMismatch>()
    }
}

val GraphValidationResult.isValid: Boolean get() = this is GraphValidationResult.Valid
val GraphValidationResult.isInvalid: Boolean get() = this is GraphValidationResult.Invalid
