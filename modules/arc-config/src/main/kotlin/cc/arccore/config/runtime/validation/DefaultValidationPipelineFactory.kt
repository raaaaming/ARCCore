package cc.arccore.config.runtime.validation

import kotlin.reflect.KClass

/**
 * Default implementation of [ValidationPipelineFactory].
 *
 * Creates a [DefaultValidationPipeline] containing an [AnnotationDrivenValidator] for the
 * given config class. Additional validators can be contributed at runtime via [addValidator].
 */
class DefaultValidationPipelineFactory : ValidationPipelineFactory {

    // Extra validators indexed by KClass — populated via addValidator()
    private val extraValidators = java.util.concurrent.ConcurrentHashMap<KClass<*>, MutableList<ConfigValidator<*>>>()

    /**
     * Registers an additional [ConfigValidator] for a specific config class.
     * It will be appended after the annotation-driven validator in the pipeline.
     */
    fun <T : Any> addValidator(clazz: KClass<T>, validator: ConfigValidator<T>) {
        extraValidators.computeIfAbsent(clazz) {
            java.util.concurrent.CopyOnWriteArrayList()
        }.add(validator)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(clazz: KClass<T>): ValidationPipeline<T> {
        val validators = mutableListOf<ConfigValidator<T>>()
        validators.add(AnnotationDrivenValidator(clazz))
        val extras = extraValidators[clazz] as? List<ConfigValidator<T>>
        if (extras != null) {
            validators.addAll(extras)
        }
        return DefaultValidationPipeline(validators)
    }
}
