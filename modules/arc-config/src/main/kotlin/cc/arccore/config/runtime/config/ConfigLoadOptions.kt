package cc.arccore.config.runtime.config

data class ConfigLoadOptions(
    val format: ConfigFormat = ConfigFormat.YAML,
    val createIfMissing: Boolean = true,
    val validateOnLoad: Boolean = true,
    val watchForChanges: Boolean = false
)
