package cc.arccore.api.module

/**
 * Marker interface for the config runtime provided to a module via [ModuleContext.config].
 *
 * The concrete implementation ([cc.arccore.config.runtime.ConfigRuntime]) lives in arc-config.
 * arc-api depends only on this marker so that the API layer remains free of config
 * implementation details.
 *
 * When no config provider is active, [ModuleContext.config] returns [NOOP].
 *
 * @since 1.0.0-Beta
 */
interface ConfigRuntimeMarker {
    companion object {
        /** Inert instance returned when no config provider is configured. */
        val NOOP: ConfigRuntimeMarker = object : ConfigRuntimeMarker {}
    }
}
