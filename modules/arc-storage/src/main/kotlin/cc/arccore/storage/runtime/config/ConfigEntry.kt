package cc.arccore.storage.runtime.config

import java.time.Instant

/**
 * Represents a single key-value configuration entry with an update timestamp.
 */
data class ConfigEntry(
    val key: String,
    val value: String,
    val updatedAt: Instant = Instant.now()
)
