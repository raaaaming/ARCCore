package cc.arccore.config.runtime.exception

class StaleConfigAccessException(
    val path: String,
    val handleGeneration: Long,
    val currentGeneration: Long
) : ConfigRuntimeException(
    "Config handle for '$path' is stale (handle generation=$handleGeneration, current=$currentGeneration). " +
        "Reload has occurred — re-load the config to get a fresh handle."
)
