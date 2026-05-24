package cc.arccore.scheduler.runtime.scheduling

import cc.arccore.scheduler.runtime.task.ScheduledTaskType

data class TaskScheduleSpec(
    val type: ScheduledTaskType,
    val delay: TickDuration = TickDuration.ZERO,
    val period: TickDuration = TickDuration.ZERO,
    val coroutineEnabled: Boolean = false,
    val description: String = ""
)
