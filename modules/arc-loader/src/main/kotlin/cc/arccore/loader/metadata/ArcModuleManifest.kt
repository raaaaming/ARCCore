package cc.arccore.loader.metadata

/**
 * Direct mapping of the `META-INF/arc-module.json` JSON structure.
 *
 * This is the raw deserialization target for Jackson.
 * Validation and conversion to [ModuleDescription] are performed
 * separately by [MetadataValidator].
 *
 * This structure mirrors what KSP will eventually generate from
 * the [ModuleSpec] annotation at compile time.
 *
 * @property id Unique module identifier (required).
 * @property name Human-readable display name.
 * @property version Semantic version string (e.g. "1.0.0", "2.1.3-beta").
 * @property main Fully qualified main class name (required).
 * @property description Free-text description of the module.
 * @property author Single author name (merged with [authors]).
 * @property authors List of authors.
 * @property depends Hard dependency expressions (module IDs or ID:versionRange).
 * @property softDepends Soft/optional dependency expressions.
 * @property loadBefore Module IDs that should load after this one.
 * @property apiVersion ARC API version requirement.
 * @property website Project website URL.
 */
data class ArcModuleManifest(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0.0",
    val main: String = "",
    val description: String = "",
    val author: String = "",
    val authors: List<String> = emptyList(),
    val depends: List<String> = emptyList(),
    val softDepends: List<String> = emptyList(),
    val loadBefore: List<String> = emptyList(),
    val dependPlugins: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
    val apiVersion: String = "1.0",
    val website: String = ""
)
