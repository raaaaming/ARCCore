package cc.arccore.runtime.annotation.registration

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.runtime.annotation.scanner.ScanResult
import kotlin.reflect.KClass

interface AutoRegistrar {
    val handledAnnotation: KClass<out Annotation>
    fun register(result: ScanResult, owner: ModuleContainerView)
}
