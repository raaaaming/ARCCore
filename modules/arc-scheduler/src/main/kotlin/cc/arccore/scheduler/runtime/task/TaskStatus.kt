package cc.arccore.scheduler.runtime.task

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED;

    fun isTerminal(): Boolean = this == COMPLETED || this == CANCELLED || this == FAILED
    fun isActive(): Boolean = this == PENDING || this == RUNNING
}
