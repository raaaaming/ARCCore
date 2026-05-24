package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext

/**
 * PRE_UNLOAD 단계: 모듈 컨텍스트에 등록된 [cc.arccore.api.module.CleanupScope]를 닫는다.
 * CleanupScope에 등록된 모든 리소스가 역순(LIFO)으로 해제된다.
 */
class PreUnloadCleanupStep : PrioritizedCleanupStep(
    priority = CleanupPriority.PRE_UNLOAD,
    body = { context ->
        val moduleContext = context.container.context
        if (moduleContext == null) {
            CleanupStepResult.Skipped
        } else {
            try {
                moduleContext.cleanupScope.close()
                CleanupStepResult.Success
            } catch (e: Exception) {
                CleanupStepResult.Failure(e)
            }
        }
    }
)
