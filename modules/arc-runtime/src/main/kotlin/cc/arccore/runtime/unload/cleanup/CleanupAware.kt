package cc.arccore.runtime.unload.cleanup

interface CleanupAware {
    fun onCleanupScheduler() {}
    fun onCleanupListener() {}
    fun onCleanupService() {}
    fun onCleanupComplete() {}
}
