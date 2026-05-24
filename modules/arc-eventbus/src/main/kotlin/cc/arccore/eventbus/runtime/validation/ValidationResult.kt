package cc.arccore.eventbus.runtime.validation

/**
 * Result of a subscription or event type validation.
 */
sealed class ValidationResult {

    /** Validation passed. */
    object Valid : ValidationResult() {
        override val isValid: Boolean = true
        override val errors: List<String> = emptyList()
        override fun toString(): String = "ValidationResult.Valid"
    }

    /**
     * Validation failed with one or more [errors].
     *
     * @param errors Human-readable descriptions of each validation failure.
     */
    data class Invalid(override val errors: List<String>) : ValidationResult() {
        override val isValid: Boolean = false

        constructor(vararg errors: String) : this(errors.toList())
    }

    abstract val isValid: Boolean
    abstract val errors: List<String>

    /**
     * Combines this result with another, merging errors if both are [Invalid].
     */
    operator fun plus(other: ValidationResult): ValidationResult = when {
        this.isValid && other.isValid -> Valid
        this.isValid -> other
        other.isValid -> this
        else -> Invalid(this.errors + other.errors)
    }
}
