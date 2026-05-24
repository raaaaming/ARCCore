package cc.arccore.config.runtime.validation

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}
