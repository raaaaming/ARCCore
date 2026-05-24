package cc.arccore.scheduler.runtime.lifecycle

fun interface SchedulerLifecycleListener {
    fun onSchedulerEvent(event: SchedulerLifecycleEvent)
}
