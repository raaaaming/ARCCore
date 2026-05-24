package cc.arccore.loader.metadata.validation

import cc.arccore.api.module.ModuleDescription

/**
 * The result of validating an [ArcModuleManifest].
 *
 * @property isValid True when there are no errors (warnings are allowed).
 * @property description The resulting [ModuleDescription] if one could be built
 *                       (may be present even with warnings).
 * @property errors All validation errors and warnings found.
 */
data class MetadataValidationResult(
    val isValid: Boolean,
    val description: ModuleDescription?,
    val errors: List<MetadataValidationError>
) {
    /** True when there are [ERROR] severity entries. */
    val hasErrors: Boolean get() = errors.any { it.severity == ValidationSeverity.ERROR }

    /** Only [WARNING] severity entries. */
    val warnings: List<MetadataValidationError> get() = errors.filter { it.severity == ValidationSeverity.WARNING }

    /** Only [ERROR] severity entries. */
    val errorEntries: List<MetadataValidationError> get() = errors.filter { it.severity == ValidationSeverity.ERROR }
}

/**
 * A single validation issue found during manifest validation.
 *
 * @property field The manifest field that triggered the issue (e.g. `"id"`, `"version"`).
 * @property message Human-readable description of the issue.
 * @property severity Whether this blocks loading ([ERROR]) or is advisory ([WARNING]).
 */
data class MetadataValidationError(
    val field: String,
    val message: String,
    val severity: ValidationSeverity
)

/**
 * Severity of a metadata validation issue.
 */
enum class ValidationSeverity {
    /** Blocks the module from being loaded. */
    ERROR,
    /** Logged but the module may still load. */
    WARNING
}
