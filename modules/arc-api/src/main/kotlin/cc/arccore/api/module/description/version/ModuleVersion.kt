package cc.arccore.api.module.description.version

data class ModuleVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) : Comparable<ModuleVersion> {

    init {
        require(major >= 0) { "Major version must be non-negative, was $major" }
        require(minor >= 0) { "Minor version must be non-negative, was $minor" }
        require(patch >= 0) { "Patch version must be non-negative, was $patch" }
    }

    override fun compareTo(other: ModuleVersion): Int {
        val coreComparison = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
        if (coreComparison != 0) return coreComparison

        val thisPre = this.preRelease
        val otherPre = other.preRelease
        if (thisPre == null && otherPre == null) return 0
        if (thisPre == null) return +1
        if (otherPre == null) return -1

        return comparePreReleaseIdentifiers(thisPre, otherPre)
    }

    override fun toString(): String {
        val base = "$major.$minor.$patch"
        val withPre = if (preRelease != null) "$base-$preRelease" else base
        return if (buildMetadata != null) "$withPre+$buildMetadata" else withPre
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModuleVersion) return false
        return compareTo(other) == 0
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(major, minor, patch, preRelease)
    }

    companion object {
        private val REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.\-]+))?(?:\+([a-zA-Z0-9.\-]+))?$""")

        fun parse(value: String): ModuleVersion {
            require(value.isNotBlank()) { "Version string must not be blank" }

            val match = REGEX.matchEntire(value.trim())
                ?: throw IllegalArgumentException(
                    "Invalid semantic version format: '$value'. Expected format: major.minor.patch[-pre-release][+build]"
                )

            return ModuleVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = match.groupValues[4].ifBlank { null },
                buildMetadata = match.groupValues[5].ifBlank { null }
            )
        }

        fun tryParse(value: String): ModuleVersion? {
            return try {
                parse(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        fun isValid(value: String): Boolean {
            if (value.isBlank()) return false
            return REGEX.matchEntire(value.trim()) != null
        }
    }
}

internal fun comparePreReleaseIdentifiers(a: String, b: String): Int {
    val aParts = a.split(".")
    val bParts = b.split(".")
    val minLen = minOf(aParts.size, bParts.size)

    for (i in 0 until minLen) {
        val aIsNumeric = aParts[i].all { it.isDigit() }
        val bIsNumeric = bParts[i].all { it.isDigit() }

        val result = when {
            aIsNumeric && bIsNumeric -> aParts[i].toInt().compareTo(bParts[i].toInt())
            aIsNumeric -> -1
            bIsNumeric -> +1
            else -> aParts[i].compareTo(bParts[i])
        }
        if (result != 0) return result
    }

    return aParts.size.compareTo(bParts.size)
}
