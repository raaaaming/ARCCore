package cc.arccore.bootstrap.runtime.preload

/**
 * Loads annotation metadata from a module's ClassLoader resources.
 * Reads META-INF/arc/arc-annotation-metadata.json if present.
 */
class AnnotationMetadataLoader {

    companion object {
        private const val ANNOTATION_METADATA_PATH = "META-INF/arc/arc-annotation-metadata.json"
    }

    /**
     * Attempts to load annotation metadata from the given ClassLoader.
     * Returns an empty cache if the metadata file is absent or malformed.
     */
    fun load(classLoader: ClassLoader): ArcAnnotationMetadataCache {
        val cache = ArcAnnotationMetadataCache()
        val resource = classLoader.getResourceAsStream(ANNOTATION_METADATA_PATH) ?: return cache

        return try {
            val json = resource.bufferedReader(Charsets.UTF_8).use { it.readText() }
            cache.cacheRawJson(ANNOTATION_METADATA_PATH, json)
            parseAnnotationMetadata(json, cache)
            cache
        } catch (_: Exception) {
            cache
        }
    }

    /**
     * Simple JSON parser for annotation metadata structure:
     * {
     *   "annotations": {
     *     "cc.arccore.api.annotation.ARCCommand": ["com.example.MyCommand", ...],
     *     ...
     *   }
     * }
     *
     * Uses manual string parsing to avoid introducing a JSON library dependency.
     */
    private fun parseAnnotationMetadata(json: String, cache: ArcAnnotationMetadataCache) {
        // Extract content of "annotations" object
        val annotationsStart = json.indexOf("\"annotations\"")
        if (annotationsStart < 0) return

        val braceStart = json.indexOf('{', annotationsStart)
        if (braceStart < 0) return

        var depth = 0
        var pos = braceStart
        val sb = StringBuilder()
        while (pos < json.length) {
            val c = json[pos]
            sb.append(c)
            if (c == '{') depth++
            else if (c == '}') {
                depth--
                if (depth == 0) break
            }
            pos++
        }

        val annotationsBlock = sb.toString()
        // Parse key -> array entries
        val keyPattern = Regex(""""([\w.$]+)"\s*:\s*\[([^\]]*)\]""")
        keyPattern.findAll(annotationsBlock).forEach { match ->
            val annotationType = match.groupValues[1]
            val classListRaw = match.groupValues[2]
            val classNames = classListRaw
                .split(',')
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
            if (classNames.isNotEmpty()) {
                cache.cacheAnnotatedClasses(annotationType, classNames)
            }
        }
    }
}
