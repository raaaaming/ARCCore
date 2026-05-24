package cc.arccore.api.module.description.version

sealed class VersionRange {

    data class Exact(val version: ModuleVersion) : VersionRange()

    data class Minimum(val version: ModuleVersion, val inclusive: Boolean = true) : VersionRange()

    data class Maximum(val version: ModuleVersion, val inclusive: Boolean = true) : VersionRange()

    data class Range(
        val min: ModuleVersion,
        val max: ModuleVersion,
        val minInclusive: Boolean,
        val maxInclusive: Boolean
    ) : VersionRange()

    data class Any(val compatible: ModuleVersion? = null) : VersionRange()

    object None : VersionRange() {
        override fun toString(): String = "None"
    }

    fun satisfiedBy(version: ModuleVersion): Boolean {
        return when (this) {
            is Exact -> version == this.version
            is Minimum -> if (inclusive) version >= this.version else version > this.version
            is Maximum -> if (inclusive) version <= this.version else version < this.version
            is Range -> {
                val minOk = if (minInclusive) version >= min else version > min
                val maxOk = if (maxInclusive) version <= max else version < max
                minOk && maxOk
            }
            is Any -> true
            is None -> false
        }
    }

    override fun toString(): String {
        return when (this) {
            is Exact -> version.toString()
            is Minimum -> "${if (inclusive) ">=" else ">"}$version"
            is Maximum -> "${if (inclusive) "<=" else "<"}$version"
            is Range -> {
                val minOp = if (minInclusive) ">=" else ">"
                val maxOp = if (maxInclusive) "<=" else "<"
                "$minOp$min $maxOp$max"
            }
            is Any -> if (compatible != null) "* (compatible: $compatible)" else "*"
            is None -> "!"
        }
    }

    companion object {
        private val OPERATORS = listOf(">=", "<=", ">", "<", "=")

        fun parse(expression: String): VersionRange {
            val trimmed = expression.trim()

            if (trimmed.isEmpty() || trimmed == "*") return Any()
            if (trimmed == "!") return None

            val hyphenRange = tryParseHyphenRange(trimmed)
            if (hyphenRange != null) return hyphenRange

            val twoPartRange = tryParseTwoPartRange(trimmed)
            if (twoPartRange != null) return twoPartRange

            return parseSingle(trimmed)
        }

        private fun tryParseHyphenRange(expression: String): Range? {
            val parts = expression.split(" - ", limit = 2)
            if (parts.size < 2) return null
            val min = ModuleVersion.parse(parts[0].trim())
            val max = ModuleVersion.parse(parts[1].trim())
            if (min > max) throw IllegalArgumentException("Invalid range: min ($min) > max ($max)")
            return Range(min, max, minInclusive = true, maxInclusive = true)
        }

        private fun tryParseTwoPartRange(expression: String): Range? {
            val tokens = expression.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.size < 2) return null

            val parsedTokens = tokens.mapNotNull { parseSingleWithOperator(it) }
            if (parsedTokens.size < 2) return null

            val first = parsedTokens[0]
            val second = parsedTokens[1]

            val minConstraint: Minimum?
            val maxConstraint: Maximum?

            when {
                first is Minimum && second is Maximum -> {
                    minConstraint = first
                    maxConstraint = second
                }
                first is Maximum && second is Minimum -> {
                    minConstraint = second
                    maxConstraint = first
                }
                else -> return null
            }

            if (minConstraint!!.version > maxConstraint!!.version) {
                throw IllegalArgumentException(
                    "Invalid range: min (${minConstraint.version}) > max (${maxConstraint.version})"
                )
            }

            return Range(
                min = minConstraint.version,
                max = maxConstraint.version,
                minInclusive = minConstraint.inclusive,
                maxInclusive = maxConstraint.inclusive
            )
        }

        private fun parseSingleWithOperator(token: String): VersionRange? {
            val op = findOperator(token)
            if (op == null) {
                val version = ModuleVersion.tryParse(token) ?: return null
                return Exact(version)
            }
            val versionStr = token.substring(op.length).trim()
            val version = ModuleVersion.tryParse(versionStr) ?: return null
            return createFromOperator(op, version)
        }

        private fun findOperator(token: String): String? {
            return OPERATORS.firstOrNull { token.startsWith(it) }
        }

        private fun createFromOperator(op: String, version: ModuleVersion): VersionRange {
            return when (op) {
                ">=" -> Minimum(version, inclusive = true)
                ">" -> Minimum(version, inclusive = false)
                "<=" -> Maximum(version, inclusive = true)
                "<" -> Maximum(version, inclusive = false)
                "=" -> Exact(version)
                else -> throw IllegalArgumentException("Unknown operator '$op'")
            }
        }

        private fun parseSingle(expression: String): VersionRange {
            val op = findOperator(expression)
            if (op != null) {
                val versionStr = expression.substring(op.length).trim()
                val version = ModuleVersion.parse(versionStr)
                return createFromOperator(op, version)
            }
            return Exact(ModuleVersion.parse(expression))
        }
    }
}
