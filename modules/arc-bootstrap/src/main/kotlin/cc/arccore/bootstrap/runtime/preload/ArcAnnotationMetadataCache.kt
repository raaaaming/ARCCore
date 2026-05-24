package cc.arccore.bootstrap.runtime.preload

import java.util.concurrent.ConcurrentHashMap

/**
 * Module-scoped cache for annotation metadata loaded from META-INF/arc/arc-annotation-metadata.json.
 * Prevents repeated ClassLoader resource lookups during bootstrap.
 */
class ArcAnnotationMetadataCache {

    private val annotatedClasses: ConcurrentHashMap<String, List<String>> = ConcurrentHashMap()
    private val rawJson: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    fun cacheAnnotatedClasses(annotationType: String, classNames: List<String>) {
        annotatedClasses[annotationType] = classNames
    }

    fun getAnnotatedClasses(annotationType: String): List<String> =
        annotatedClasses[annotationType] ?: emptyList()

    fun cacheRawJson(key: String, json: String) {
        rawJson[key] = json
    }

    fun getRawJson(key: String): String? = rawJson[key]

    fun isEmpty(): Boolean = annotatedClasses.isEmpty() && rawJson.isEmpty()

    fun allAnnotationTypes(): Set<String> = annotatedClasses.keys.toSet()

    fun totalAnnotatedClassCount(): Int = annotatedClasses.values.sumOf { it.size }
}
