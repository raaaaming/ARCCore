package cc.arccore.config.runtime.serializer

/**
 * Simple YAML writer for flat and one-level nested [Map] data.
 *
 * No external library dependency. Sufficient for serializing Minecraft plugin
 * config data classes back to YAML text.
 */
object YamlWriter {

    /**
     * Serializes the given map to a YAML string.
     *
     * Nested maps (one level) are rendered as YAML sections.
     * All other values are rendered inline.
     */
    fun write(data: Map<String, Any?>, indent: Int = 0): String {
        val sb = StringBuilder()
        val prefix = "  ".repeat(indent)
        for ((key, value) in data) {
            when (value) {
                null -> sb.appendLine("${prefix}${key}: ~")
                is Map<*, *> -> {
                    sb.appendLine("${prefix}${key}:")
                    @Suppress("UNCHECKED_CAST")
                    sb.append(write(value as Map<String, Any?>, indent + 1))
                }
                is String -> {
                    // Quote strings that could be misinterpreted
                    if (needsQuoting(value)) {
                        sb.appendLine("${prefix}${key}: \"${escapeString(value)}\"")
                    } else {
                        sb.appendLine("${prefix}${key}: $value")
                    }
                }
                is Boolean -> sb.appendLine("${prefix}${key}: $value")
                is Number -> sb.appendLine("${prefix}${key}: $value")
                is List<*> -> {
                    if (value.isEmpty()) {
                        sb.appendLine("${prefix}${key}: []")
                    } else {
                        sb.appendLine("${prefix}${key}:")
                        for (item in value) {
                            sb.appendLine("${prefix}  - ${renderScalar(item)}")
                        }
                    }
                }
                else -> sb.appendLine("${prefix}${key}: ${value.toString()}")
            }
        }
        return sb.toString()
    }

    private fun renderScalar(value: Any?): String {
        return when (value) {
            null -> "~"
            is String -> if (needsQuoting(value)) "\"${escapeString(value)}\"" else value
            else -> value.toString()
        }
    }

    private fun needsQuoting(s: String): Boolean {
        if (s.isEmpty()) return true
        // Quote if it could be mistaken for a boolean, number, null, or contains special chars
        val lower = s.lowercase()
        if (lower == "true" || lower == "false" || lower == "null" || lower == "~") return true
        if (s.toIntOrNull() != null || s.toDoubleOrNull() != null || s.toLongOrNull() != null) return true
        if (s.any { it == ':' || it == '#' || it == '\n' || it == '"' || it == '\'' }) return true
        return false
    }

    private fun escapeString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
