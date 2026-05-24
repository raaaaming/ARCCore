package cc.arccore.runtime.unload.cleanup

import cc.arccore.api.service.ServiceRegistry
import cc.arccore.runtime.unload.CleanupContext
import java.util.logging.Logger

class ServiceRegistryCleanupStep(
    private val serviceRegistry: ServiceRegistry
) : PrioritizedCleanupStep(
    priority = CleanupPriority.SERVICE_REGISTRY,
    body = serviceRegistryCleanupBody(serviceRegistry)
) {
    companion object {
        private val log = Logger.getLogger(ServiceRegistryCleanupStep::class.java.name)

        private fun serviceRegistryCleanupBody(
            serviceRegistry: ServiceRegistry
        ): (CleanupContext) -> CleanupStepResult = { context ->
            try {
                // unregisterAllById: ClassLoader 참조 없이 moduleId(String)만으로 해제.
                // 이 단계(SERVICE_REGISTRY=310)는 CLASSLOADER(600) 이전에 실행되므로
                // owner 참조 없이도 안전하게 동작한다.
                val removed = serviceRegistry.unregisterAllById(context.moduleId)
                if (removed > 0) {
                    log.fine("Unregistered $removed service(s) for module '${context.moduleId}'")
                }
                CleanupStepResult.Success
            } catch (e: Exception) {
                log.warning("Failed to unregister services for '${context.moduleId}': ${e.message}")
                CleanupStepResult.Failure(e)
            }
        }
    }
}
