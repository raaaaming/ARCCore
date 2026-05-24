package cc.arccore.migration.runtime.model

internal class MigrationDrainRecord {
    @Volatile var drainStartMs: Long = 0L
    @Volatile var drainEndMs: Long = 0L
    @Volatile var inflightAtStart: Int = 0
    @Volatile var forceDrained: Boolean = false

    val drainDurationMs: Long get() = if (drainEndMs > 0) drainEndMs - drainStartMs else 0L
}
