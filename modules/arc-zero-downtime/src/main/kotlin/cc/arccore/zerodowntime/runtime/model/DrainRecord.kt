package cc.arccore.zerodowntime.runtime.model

data class DrainRecord(
    var inflightTasksAtStart: Int = 0,
    var drainStartMs: Long = 0L,
    var drainEndMs: Long = 0L,
    var forceDrained: Boolean = false
) {
    val drainDurationMs: Long get() = if (drainEndMs > 0) drainEndMs - drainStartMs else 0L
}
