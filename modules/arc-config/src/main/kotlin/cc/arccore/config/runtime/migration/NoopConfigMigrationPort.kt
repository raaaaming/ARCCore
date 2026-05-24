package cc.arccore.config.runtime.migration

/**
 * Pass-through [ConfigMigrationPort] that returns the data unchanged.
 *
 * Used when no schema migration logic is needed (the common case in Beta).
 */
object NoopConfigMigrationPort : ConfigMigrationPort {
    override fun migrate(data: Map<String, Any?>, fromVersion: Int, toVersion: Int): Map<String, Any?> = data
}
