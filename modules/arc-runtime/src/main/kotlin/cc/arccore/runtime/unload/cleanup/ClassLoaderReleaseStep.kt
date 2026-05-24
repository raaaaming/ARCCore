package cc.arccore.runtime.unload.cleanup

import cc.arccore.runtime.unload.CleanupContext
import cc.arccore.runtime.unload.leak.ModuleClassLoaderTracker

/**
 * CLASSLOADER_RELEASE 단계: ClassLoader.close() 이후 [ModuleClassLoaderTracker]에 등록하여
 * GC 수집 여부를 지연 감시한다.
 * ClassLoader가 null이거나 아직 close되지 않은 경우 skip한다.
 */
class ClassLoaderReleaseStep(
    private val tracker: ModuleClassLoaderTracker
) : PrioritizedCleanupStep(
    priority = CleanupPriority.CLASSLOADER_RELEASE,
    body = { context ->
        val cl = context.classLoader
        if (cl != null && cl.isClosed()) {
            try {
                tracker.track(context.moduleId, cl)
                CleanupStepResult.Success
            } catch (e: Exception) {
                CleanupStepResult.Failure(e)
            }
        } else {
            CleanupStepResult.Skipped
        }
    }
)
