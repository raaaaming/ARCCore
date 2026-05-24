package cc.arccore.config.runtime.migration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigSchemaVersion(val version: Int = 1)
