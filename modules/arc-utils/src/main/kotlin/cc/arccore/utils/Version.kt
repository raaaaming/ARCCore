package cc.arccore.utils

data class Version(val major: Int, val minor: Int, val patch: Int) :
    Comparable<Version> {

    override fun compareTo(other: Version): Int {
        return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val REGEX = Regex("(\\d+)\\.(\\d+)(?:\\.(\\d+))?")

        fun parse(value: String): Version {
            val match = REGEX.find(value)
            requireNotNull(match) { "Invalid version format: $value" }
            return Version(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toIntOrNull() ?: 0
            )
        }
    }
}
