package cc.arccore.runtime.reload.rollback

import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.lifecycle.ModuleRuntime
import cc.arccore.runtime.reload.ReloadContext
import java.util.logging.Logger

class ReloadRollbackManager(private val runtime: ModuleRuntime) {

    private val log = Logger.getLogger(ReloadRollbackManager::class.java.name)

    data class RollbackResult(
        val restoredModules: List<String>,
        val failedRestores: List<Pair<String, Throwable>>
    ) {
        val partialSuccess: Boolean get() = restoredModules.isNotEmpty()
    }

    internal fun rollback(context: ReloadContext): RollbackResult {
        val restored = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, Throwable>>()

        val toRestore = context.disabledModules
            .filter { it != context.targetModuleId }
            .reversed()

        for (moduleId in toRestore) {
            val container = runtime.getContainer(moduleId)
            if (container == null) {
                log.warning("[Rollback] Cannot re-enable '$moduleId': container not found")
                continue
            }
            if (container.state == ModuleState.FAILED || container.state == ModuleState.UNLOADED) {
                log.warning("[Rollback] Skipping '$moduleId': state=${container.state}")
                continue
            }
            try {
                val result = runtime.enableModule(moduleId)
                if (result.isSuccess) {
                    restored.add(moduleId)
                    log.info("[Rollback] Re-enabled '$moduleId' successfully")
                } else {
                    val err = RuntimeException("Re-enable failed for '$moduleId'")
                    failed.add(moduleId to err)
                    log.warning("[Rollback] Failed to re-enable '$moduleId'")
                }
            } catch (e: Exception) {
                failed.add(moduleId to e)
                log.warning("[Rollback] Exception re-enabling '$moduleId': ${e.message}")
            }
        }

        return RollbackResult(restored, failed)
    }
}
