package cc.arccore.snapshot.runtime.lifecycle

fun interface SnapshotLifecycleObserver {
    fun onSnapshotEvent(event: SnapshotLifecycleEvent)
}
