package cc.arccore.runtime.annotation.scanner

import kotlin.reflect.KClass

interface AnnotationScanner {
    fun scan(
        classLoader: ClassLoader,
        moduleId: String,
        targetAnnotations: Set<KClass<out Annotation>>
    ): List<ScanResult>
}
