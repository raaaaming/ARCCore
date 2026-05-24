package cc.arccore.migration.runtime.lifecycle

fun interface MigrationLifecycleObserver {
    fun onMigrationEvent(event: MigrationLifecycleEvent)
}
