package cc.arccore.bootstrap.runtime

/**
 * Type-safe key for values stored in [BootstrapContext]'s slot map.
 *
 * Keys are defined as companion object constants so that all phases and handlers
 * use the exact same key instances — no string typo risk.
 */
class BootstrapContextKey<T : Any>(val name: String) {

    override fun toString(): String = "BootstrapContextKey($name)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BootstrapContextKey<*>) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    companion object {
        /**
         * Set by [cc.arccore.bootstrap.runtime.preload.MetadataPreloader] during METADATA_PRELOAD.
         */
        val PRELOADED_METADATA =
            BootstrapContextKey<cc.arccore.bootstrap.runtime.preload.PreloadedModuleMetadata>("preloaded_metadata")

        /**
         * Set by the dependency graph build handler during DEPENDENCY_GRAPH_BUILD.
         * Contains module IDs in topological load order.
         */
        val DEPENDENCY_ORDER =
            BootstrapContextKey<List<String>>("dependency_order")

        /**
         * Set by [cc.arccore.bootstrap.runtime.integration.GeneratedRegistrarBridge]
         * during GENERATED_BOOTSTRAP.
         */
        val REGISTRAR_RESULT =
            BootstrapContextKey<cc.arccore.bootstrap.runtime.integration.GeneratedRegistrarBridge.BootstrapRegistrarResult>(
                "registrar_result"
            )

        /**
         * Set by [cc.arccore.bootstrap.runtime.lazy.LazyWiringContext] during SERVICE_WIRING.
         */
        val WIRING_RESULT =
            BootstrapContextKey<cc.arccore.bootstrap.runtime.lazy.ServiceWiringResult>("wiring_result")

        /**
         * The raw generated object graph (type-erased to [Any]) placed by
         * [cc.arccore.bootstrap.runtime.integration.GeneratedInjectorBridge].
         */
        val GENERATED_OBJECT_GRAPH =
            BootstrapContextKey<Any>("generated_object_graph")
    }
}
