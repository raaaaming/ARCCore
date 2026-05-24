package cc.arccore.migration.runtime.model

import java.util.UUID

@JvmInline
value class MigrationId(val value: String) {
    companion object {
        fun generate(moduleId: String): MigrationId =
            MigrationId("migration-$moduleId-${UUID.randomUUID()}")

        fun of(raw: String): MigrationId = MigrationId(raw)
    }
}
