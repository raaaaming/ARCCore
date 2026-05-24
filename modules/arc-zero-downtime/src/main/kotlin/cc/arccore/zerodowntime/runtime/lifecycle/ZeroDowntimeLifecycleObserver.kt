package cc.arccore.zerodowntime.runtime.lifecycle

fun interface ZeroDowntimeLifecycleObserver {
    fun onZeroDowntimeEvent(event: ZeroDowntimeLifecycleEvent)
}
