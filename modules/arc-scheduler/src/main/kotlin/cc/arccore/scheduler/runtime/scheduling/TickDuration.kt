package cc.arccore.scheduler.runtime.scheduling

@JvmInline
value class TickDuration(val ticks: Long) : Comparable<TickDuration> {
    init { require(ticks >= 0) { "TickDuration cannot be negative: $ticks" } }

    operator fun plus(other: TickDuration) = TickDuration(ticks + other.ticks)
    operator fun minus(other: TickDuration) = TickDuration(maxOf(0, ticks - other.ticks))
    operator fun times(factor: Long) = TickDuration(ticks * factor)

    override fun compareTo(other: TickDuration) = ticks.compareTo(other.ticks)

    fun toMillis(msPerTick: Long = 50L) = ticks * msPerTick

    override fun toString() = "${ticks}t"

    companion object {
        val ZERO = TickDuration(0)
        val ONE_SECOND = TickDuration(20)
        val ONE_MINUTE = TickDuration(1200)
    }
}
