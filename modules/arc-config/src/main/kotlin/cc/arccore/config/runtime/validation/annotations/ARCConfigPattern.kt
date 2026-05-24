package cc.arccore.config.runtime.validation.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ARCConfigPattern(val regex: String)
