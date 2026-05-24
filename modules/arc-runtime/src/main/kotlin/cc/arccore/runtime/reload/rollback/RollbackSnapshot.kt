package cc.arccore.runtime.reload.rollback

import cc.arccore.api.module.ModuleState

data class RollbackSnapshot(
    val moduleId: String,
    val stateBeforeReload: ModuleState,
    val capturedState: Map<String, Any?>?
)
