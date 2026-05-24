package cc.arccore.config.runtime.validation

class DefaultValidationPipeline<T : Any>(
    private val validators: List<ConfigValidator<T>>
) : ValidationPipeline<T> {
    override fun validate(value: T): ValidationResult {
        val errors = validators.flatMap { v ->
            when (val r = v.validate(value)) {
                is ValidationResult.Valid -> emptyList()
                is ValidationResult.Invalid -> r.errors
            }
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
