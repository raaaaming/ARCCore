package cc.arccore.config.runtime.validation

import kotlin.reflect.KClass

interface ValidationPipelineFactory {
    fun <T : Any> create(clazz: KClass<T>): ValidationPipeline<T>
}
