package cc.arccore.bootstrap.runtime.preload

/**
 * Describes the presence and version of KSP-generated bootstrap artifacts
 * for a given module's ClassLoader.
 */
data class GeneratedArtifactManifest(
    val hasArcBootstrapClass: Boolean,
    val hasInjectorsList: Boolean,
    val hasAnnotationMetadata: Boolean,
    val arcBootstrapClassName: String?,
    val injectorClassNames: List<String>,
    val metadataVersion: Int,
    val generatedAt: String?
) {
    val isFullyGenerated: Boolean
        get() = hasArcBootstrapClass && hasInjectorsList && hasAnnotationMetadata

    val isPartiallyGenerated: Boolean
        get() = hasArcBootstrapClass || hasInjectorsList || hasAnnotationMetadata

    val isMissing: Boolean
        get() = !hasArcBootstrapClass && !hasInjectorsList && !hasAnnotationMetadata

    companion object {
        val MISSING = GeneratedArtifactManifest(
            hasArcBootstrapClass = false,
            hasInjectorsList = false,
            hasAnnotationMetadata = false,
            arcBootstrapClassName = null,
            injectorClassNames = emptyList(),
            metadataVersion = 0,
            generatedAt = null
        )
    }
}
