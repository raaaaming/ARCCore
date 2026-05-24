package cc.arccore.bootstrap.runtime.preload

import cc.arccore.api.module.ModuleDescription

/**
 * All metadata loaded during the METADATA_PRELOAD phase for a single module.
 * Stored into BootstrapContext via BootstrapContextKey.PRELOADED_METADATA.
 */
data class PreloadedModuleMetadata(
    val moduleId: String,
    val description: ModuleDescription,
    val artifactManifest: GeneratedArtifactManifest,
    val annotationCache: ArcAnnotationMetadataCache,
    val rawModuleJson: String?,
    val preloadedAtNanos: Long = System.nanoTime()
) {
    val hasGeneratedArtifacts: Boolean get() = artifactManifest.isPartiallyGenerated
    val hasFullGeneratedArtifacts: Boolean get() = artifactManifest.isFullyGenerated
}
