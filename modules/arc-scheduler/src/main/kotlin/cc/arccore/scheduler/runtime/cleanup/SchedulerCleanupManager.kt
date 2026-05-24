package cc.arccore.scheduler.runtime.cleanup

import cc.arccore.scheduler.runtime.cancellation.CancellationReason
import cc.arccore.scheduler.runtime.integration.ContextSchedulerAccessor
import cc.arccore.scheduler.runtime.ownership.OrphanTaskDetector
import cc.arccore.scheduler.runtime.ownership.OrphanTaskReport
import cc.arccore.scheduler.runtime.ownership.TaskOwnershipRegistry
import cc.arccore.scheduler.runtime.state.SchedulerStateRegistry

data class SchedulerLeakReport(
    val moduleId: String,
    val orphanReport: OrphanTaskReport?,
    val cancelledCount: Int
)

class SchedulerCleanupManager internal constructor(
    private val ownershipRegistry: TaskOwnershipRegistry,
    private val stateRegistry: SchedulerStateRegistry,
    private val orphanDetector: OrphanTaskDetector? = null
) {
    fun cleanup(moduleId: String): SchedulerLeakReport {
        stateRegistry.startDraining(moduleId)

        val orphanReport = orphanDetector?.detect(moduleId)
        val activeCount = ownershipRegistry.activeTaskCount(moduleId)

        ownershipRegistry.cancelAllForModule(moduleId, CancellationReason.MODULE_UNLOAD)

        stateRegistry.close(moduleId)
        ContextSchedulerAccessor.remove(moduleId)

        return SchedulerLeakReport(
            moduleId = moduleId,
            orphanReport = orphanReport,
            cancelledCount = activeCount
        )
    }

    fun reloadCleanup(moduleId: String): SchedulerLeakReport {
        val activeCount = ownershipRegistry.activeTaskCount(moduleId)
        ownershipRegistry.cancelAllForModule(moduleId, CancellationReason.MODULE_RELOAD)

        return SchedulerLeakReport(
            moduleId = moduleId,
            orphanReport = orphanDetector?.detect(moduleId),
            cancelledCount = activeCount
        )
    }
}
