package cc.arccore.config.runtime.metadata

import cc.arccore.config.runtime.config.ConfigFormat
import kotlin.reflect.KClass

data class ConfigMetadata<T : Any>(
    val configClass: KClass<T>,
    val defaultPath: String,
    val format: ConfigFormat = ConfigFormat.YAML,
    val schemaVersion: Int = 1,
    val description: String = ""
)
