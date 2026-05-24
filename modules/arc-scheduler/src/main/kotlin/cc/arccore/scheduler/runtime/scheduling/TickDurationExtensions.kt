package cc.arccore.scheduler.runtime.scheduling

val Int.ticks: TickDuration get() = TickDuration(this.toLong())
val Long.ticks: TickDuration get() = TickDuration(this)

val Int.seconds: TickDuration get() = TickDuration(this * 20L)
val Long.seconds: TickDuration get() = TickDuration(this * 20L)

val Int.minutes: TickDuration get() = TickDuration(this * 1200L)
val Long.minutes: TickDuration get() = TickDuration(this * 1200L)
