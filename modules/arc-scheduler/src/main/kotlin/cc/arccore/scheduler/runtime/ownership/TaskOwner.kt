package cc.arccore.scheduler.runtime.ownership

data class TaskOwner(
    val moduleId: String,
    val generation: Int = 0
)
