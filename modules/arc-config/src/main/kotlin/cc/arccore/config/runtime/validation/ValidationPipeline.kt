package cc.arccore.config.runtime.validation

interface ValidationPipeline<T : Any> {
    fun validate(value: T): ValidationResult
}
