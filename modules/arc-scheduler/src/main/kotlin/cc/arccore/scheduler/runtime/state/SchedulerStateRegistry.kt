package cc.arccore.scheduler.runtime.state

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

internal class SchedulerStateRegistry {
    private val states = ConcurrentHashMap<String, AtomicReference<SchedulerState>>()

    fun initialize(moduleId: String) {
        states[moduleId] = AtomicReference(SchedulerState.INITIALIZING)
    }

    fun activate(moduleId: String) {
        states[moduleId]?.compareAndSet(SchedulerState.INITIALIZING, SchedulerState.ACTIVE)
    }

    fun startDraining(moduleId: String): Boolean {
        val ref = states[moduleId] ?: return false
        return ref.compareAndSet(SchedulerState.ACTIVE, SchedulerState.DRAINING)
    }

    fun close(moduleId: String) {
        states[moduleId]?.set(SchedulerState.CLOSED)
        states.remove(moduleId)
    }

    fun getState(moduleId: String): SchedulerState =
        states[moduleId]?.get() ?: SchedulerState.CLOSED

    fun canSchedule(moduleId: String): Boolean =
        getState(moduleId).canSchedule()
}
