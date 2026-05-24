package cc.arccore.storage.runtime.validation

/**
 * Result of a storage access or configuration validation check.
 */
sealed class StorageValidationResult {

    /** Validation passed without issues. */
    object Valid : StorageValidationResult()

    /**
     * Validation failed.
     *
     * @property reason Human-readable description of the failure.
     */
    data class Invalid(val reason: String) : StorageValidationResult()

    /** Returns `true` if this result is [Valid]. */
    val isValid: Boolean get() = this is Valid
}
