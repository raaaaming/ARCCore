package cc.arccore.runtime.resource

/**
 * Identifies the module that owns a tracked resource.
 * Every resource in the ownership graph must have an owner — orphan resources are a bug.
 */
data class ResourceOwner(
    val moduleId: String,
    val moduleName: String? = null
) {
    override fun toString(): String = moduleName?.let { "$it ($moduleId)" } ?: moduleId
}
