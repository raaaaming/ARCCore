package cc.arccore.api.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ARCService(val name: String = "")
