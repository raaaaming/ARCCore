package cc.arccore.runtime.dependency

import cc.arccore.api.module.description.version.ModuleVersion
import cc.arccore.api.module.description.version.VersionRange

sealed class DependencyViolation {

    abstract val severity: Severity

    enum class Severity { ERROR, WARNING }

    data class CircularDependency(val cycle: List<String>) : DependencyViolation() {
        override val severity: Severity = Severity.ERROR

        fun cycleAsString(): String = cycle.joinToString(" → ") + " → ${cycle.first()}"
    }

    data class MissingDependency(
        val dependentId: String,
        val missingId: String
    ) : DependencyViolation() {
        override val severity: Severity = Severity.ERROR
    }

    data class VersionMismatch(
        val dependentId: String,
        val dependencyId: String,
        val required: VersionRange,
        val actual: ModuleVersion
    ) : DependencyViolation() {
        override val severity: Severity = Severity.ERROR
    }

    data class MissingSoftDependency(
        val dependentId: String,
        val missingId: String
    ) : DependencyViolation() {
        override val severity: Severity = Severity.WARNING
    }
}
