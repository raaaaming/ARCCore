package cc.arccore.config.runtime.migration

interface ConfigMigrationPort {
    fun migrate(data: Map<String, Any?>, fromVersion: Int, toVersion: Int): Map<String, Any?>
}
