package cc.arccore.config.runtime.config

import java.time.Instant

data class ConfigEntry<T : Any>(
    val moduleId: String,
    val path: String,
    val value: T,
    val format: ConfigFormat,
    val generation: Long,
    val loadedAt: Instant = Instant.now(),
    val reloadCount: Int = 0
)
