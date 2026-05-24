package cc.arccore.migration.runtime.model

import java.time.Instant

data class MigrationMetrics(
    val totalMigrations: Long,
    val successfulMigrations: Long,
    val failedMigrations: Long,
    val rolledBackMigrations: Long,
    val abortedMigrations: Long,
    val averageMigrationMs: Double,
    val averageDrainMs: Double,
    val averageSnapshotSizeBytes: Double,
    val lastMigrationAt: Instant?
)
