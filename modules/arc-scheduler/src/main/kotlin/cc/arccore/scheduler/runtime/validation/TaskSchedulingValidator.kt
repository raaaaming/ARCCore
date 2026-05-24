package cc.arccore.scheduler.runtime.validation

import cc.arccore.scheduler.runtime.exception.ModuleUnloadedException
import cc.arccore.scheduler.runtime.exception.SchedulingException
import cc.arccore.scheduler.runtime.scheduling.TickDuration

class TaskSchedulingValidator {
    fun validateCanSchedule(moduleId: String, isActive: Boolean) {
        if (!isActive) throw ModuleUnloadedException(moduleId)
    }

    fun validatePeriod(moduleId: String, period: TickDuration) {
        if (period.ticks <= 0) {
            throw SchedulingException(moduleId, "Repeating task period must be > 0, got ${period.ticks}")
        }
    }

    fun validateDelay(moduleId: String, delay: TickDuration) {
        if (delay.ticks < 0) {
            throw SchedulingException(moduleId, "Task delay cannot be negative, got ${delay.ticks}")
        }
    }
}
