package cc.arccore.config.runtime.serializer

/**
 * Minimal YAML reader with NO external library dependency.
 *
 * Handles flat and two-level nested key-value YAML files sufficient for
 * simple Minecraft plugin config data classes.
 *
 * Supported features:
 * - Blank lines and `#` comment lines are ignored
 * - `key: value` pairs at the root level
 * - Nested sections via 2-space (or any consistent) indentation, one level deep
 * - Value type inference: booleans, integers, doubles, null, quoted strings, plain strings
 * - Multi-line values are NOT supported
 */
object YamlRawReader {

    fun parse(content: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val lines = content.lines()

        var currentSection: String? = null
        var currentSectionMap: MutableMap<String, Any?>? = null

        for (rawLine in lines) {
            // Strip trailing whitespace but preserve leading (indentation check)
            val line = rawLine.trimEnd()

            // Skip blank lines
            if (line.isBlank()) continue

            // Skip comment-only lines
            val commentStripped = stripInlineComment(line)
            if (commentStripped.isBlank()) continue

            // Determine indentation level
            val indent = line.length - line.trimStart().length
            val trimmed = commentStripped.trimStart()

            if (indent > 0) {
                // Child key of current section
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx < 0) continue

                val key = trimmed.substring(0, colonIdx).trim()
                val rawValue = if (colonIdx + 1 < trimmed.length) trimmed.substring(colonIdx + 1).trim() else ""

                if (currentSection != null && currentSectionMap != null) {
                    currentSectionMap[key] = parseValue(rawValue)
                }
            } else {
                // Root-level key
                // Flush any pending section
                if (currentSection != null && currentSectionMap != null) {
                    result[currentSection] = currentSectionMap
                }
                currentSection = null
                currentSectionMap = null

                val colonIdx = trimmed.indexOf(':')
                if (colonIdx < 0) continue

                val key = trimmed.substring(0, colonIdx).trim()
                val rawValue = if (colonIdx + 1 < trimmed.length) trimmed.substring(colonIdx + 1).trim() else ""

                if (rawValue.isEmpty()) {
                    // Section start
                    currentSection = key
                    currentSectionMap = mutableMapOf()
                } else {
                    result[key] = parseValue(rawValue)
                }
            }
        }

        // Flush last section
        if (currentSection != null && currentSectionMap != null) {
            result[currentSection] = currentSectionMap
        }

        return result
    }

    /**
     * Returns a flat map where nested sections are represented as "parent.child" keys.
     * Useful for simpler property-style access.
     */
    fun parseFlat(content: String): Map<String, Any?> {
        val nested = parse(content)
        val flat = mutableMapOf<String, Any?>()
        for ((k, v) in nested) {
            if (v is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                for ((ck, cv) in v as Map<String, Any?>) {
                    flat["$k.$ck"] = cv
                }
            } else {
                flat[k] = v
            }
        }
        return flat
    }

    private fun stripInlineComment(line: String): String {
        // Remove inline comments that are not inside quotes
        // Simple heuristic: find ' #' that is not inside a quoted string
        var inSingleQuote = false
        var inDoubleQuote = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                ch == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                ch == '#' && !inSingleQuote && !inDoubleQuote -> {
                    // Anything after this is a comment
                    break
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    private fun parseValue(raw: String): Any? {
        if (raw.isEmpty() || raw == "~" || raw.lowercase() == "null") return null

        // Quoted strings — strip quotes
        if ((raw.startsWith('"') && raw.endsWith('"')) ||
            (raw.startsWith('\'') && raw.endsWith('\''))
        ) {
            return raw.substring(1, raw.length - 1)
        }

        // Booleans
        if (raw.lowercase() == "true") return true
        if (raw.lowercase() == "false") return false

        // Try integer
        raw.toIntOrNull()?.let { return it }

        // Try long
        raw.toLongOrNull()?.let { return it }

        // Try double
        raw.toDoubleOrNull()?.let { return it }

        // Plain string
        return raw
    }
}
