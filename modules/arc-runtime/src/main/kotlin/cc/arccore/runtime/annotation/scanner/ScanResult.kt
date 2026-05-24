package cc.arccore.runtime.annotation.scanner

import kotlin.reflect.KClass

data class ScanResult(
    val clazz: Class<*>,
    val annotationType: KClass<out Annotation>,
    val moduleId: String
)
