package cc.arccore.runtime.annotation.scanner

import kotlin.reflect.KClass

class MetadataScanner(
    private val fallback: AnnotationScanner = ReflectionScanner()
) : AnnotationScanner {

    companion object {
        private val annotationToPath = mapOf(
            "cc.arccore.api.annotation.ARCCommand" to "META-INF/arc/commands.json",
            "cc.arccore.api.annotation.ARCListener" to "META-INF/arc/listeners.json",
            "cc.arccore.api.annotation.ARCService" to "META-INF/arc/services.json"
        )
    }

    override fun scan(
        classLoader: ClassLoader,
        moduleId: String,
        targetAnnotations: Set<KClass<out Annotation>>
    ): List<ScanResult> {
        val results = mutableListOf<ScanResult>()
        val fallbackAnnotations = mutableSetOf<KClass<out Annotation>>()

        for (annotation in targetAnnotations) {
            val fqn = annotation.qualifiedName
            val path = if (fqn != null) annotationToPath[fqn] else null

            if (path == null) {
                fallbackAnnotations.add(annotation)
                continue
            }

            val stream = classLoader.getResourceAsStream(path)
            if (stream == null) {
                fallbackAnnotations.add(annotation)
                continue
            }

            try {
                val content = stream.use { it.bufferedReader().readText() }
                parseClassNames(content).forEach { className ->
                    try {
                        results.add(ScanResult(classLoader.loadClass(className), annotation, moduleId))
                    } catch (_: Throwable) {}
                }
            } catch (_: Exception) {
                fallbackAnnotations.add(annotation)
            }
        }

        if (fallbackAnnotations.isNotEmpty()) {
            results.addAll(fallback.scan(classLoader, moduleId, fallbackAnnotations))
        }

        return results
    }

    private fun parseClassNames(json: String): List<String> =
        Regex(""""className"\s*:\s*"([^"]+)"""")
            .findAll(json)
            .map { it.groupValues[1] }
            .toList()
}
