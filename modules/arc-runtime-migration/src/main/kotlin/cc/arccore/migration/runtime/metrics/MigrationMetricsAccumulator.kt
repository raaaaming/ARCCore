package cc.arccore.migration.runtime.metrics

import cc.arccore.migration.runtime.model.MigrationMetrics
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class MigrationMetricsAccumulator {
    private val totalMigrations = AtomicLong()
    private val successful = AtomicLong()
    private val failed = AtomicLong()
    private val rolledBack = AtomicLong()
    private val aborted = AtomicLong()
    private val totalMigrationMs = AtomicLong()
    private val totalDrainMs = AtomicLong()
    private val totalSnapshotBytes = AtomicLong()
    private val lastMigrationAt = AtomicReference<Instant?>(null)

    fun recordSuccess(migrationMs: Long, drainMs: Long, snapshotBytes: Long) {
        totalMigrations.incrementAndGet()
        successful.incrementAndGet()
        totalMigrationMs.addAndGet(migrationMs)
        totalDrainMs.addAndGet(drainMs)
        totalSnapshotBytes.addAndGet(snapshotBytes)
        lastMigrationAt.set(Instant.now())
    }

    fun recordFailure(wasRolledBack: Boolean) {
        totalMigrations.incrementAndGet()
        failed.incrementAndGet()
        if (wasRolledBack) {
            rolledBack.incrementAndGet()
        }
        lastMigrationAt.set(Instant.now())
    }

    fun recordAborted() {
        totalMigrations.incrementAndGet()
        aborted.incrementAndGet()
        lastMigrationAt.set(Instant.now())
    }

    fun snapshot(): MigrationMetrics {
        val total = totalMigrations.get()
        val successCount = successful.get()
        val avgMigrationMs = if (successCount > 0) totalMigrationMs.get().toDouble() / successCount else 0.0
        val avgDrainMs = if (successCount > 0) totalDrainMs.get().toDouble() / successCount else 0.0
        val avgSnapshotBytes = if (successCount > 0) totalSnapshotBytes.get().toDouble() / successCount else 0.0

        return MigrationMetrics(
            totalMigrations = total,
            successfulMigrations = successCount,
            failedMigrations = failed.get(),
            rolledBackMigrations = rolledBack.get(),
            abortedMigrations = aborted.get(),
            averageMigrationMs = avgMigrationMs,
            averageDrainMs = avgDrainMs,
            averageSnapshotSizeBytes = avgSnapshotBytes,
            lastMigrationAt = lastMigrationAt.get()
        )
    }
}
