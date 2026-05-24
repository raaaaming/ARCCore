package cc.arccore.config.runtime.validation

interface ConfigValidator<T : Any> {
    fun validate(value: T): ValidationResult
}
