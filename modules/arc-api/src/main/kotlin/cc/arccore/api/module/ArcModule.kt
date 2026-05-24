package cc.arccore.api.module

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModuleSpec(
    val id: String,
    val name: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val authors: Array<String> = [],
    val dependencies: Array<String> = [],
    val providesServices: Array<String> = []
)
