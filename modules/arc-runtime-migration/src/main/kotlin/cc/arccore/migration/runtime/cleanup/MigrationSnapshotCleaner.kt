package cc.arccore.migration.runtime.cleanup

import cc.arccore.migration.runtime.model.MigrationId
import java.util.concurrent.ConcurrentHashMap

internal class MigrationSnapshotCleaner {
    private val cleanedSnapshots = ConcurrentHashMap.newKeySet<String>()

    fun deleteMigrationSnapshot(snapshotId: String): Boolean {
        return cleanedSnapshots.add(snapshotId)
    }

    fun cleanupAll(migrationId: MigrationId): Int {
        val id = migrationId.value
        if (cleanedSnapshots.add(id)) return 1
        return 0
    }
}
