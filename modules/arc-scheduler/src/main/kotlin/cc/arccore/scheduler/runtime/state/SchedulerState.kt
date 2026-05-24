package cc.arccore.scheduler.runtime.state

enum class SchedulerState {
    INITIALIZING,
    ACTIVE,
    DRAINING,
    CLOSED;

    fun canSchedule(): Boolean = this == ACTIVE
    fun isTerminal(): Boolean = this == CLOSED
}
