package cc.arccore.runtime.lifecycle

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.api.module.ModuleContainer
import cc.arccore.api.module.ModuleContainerView
import java.util.logging.Logger

/**
 * FAILED 이벤트를 구독하여, 실패한 모듈을 필수 의존하는
 * 다른 모듈들을 자동으로 disable합니다.
 *
 * optional 의존성(soft dependency)은 전파 대상에서 제외됩니다.
 * 전파 중 추가 실패가 발생해도 로그 후 계속 진행합니다(best-effort).
 */
class DependencyFailurePropagator(
    private val orchestrator: LifecycleOrchestrator,
    private val lifecycleManager: ModuleLifecycleManager,
    private val eventBus: LifecycleEventBus,
    private val containersProvider: () -> Collection<ModuleContainer>
) : LifecycleObserver {

    private val log = Logger.getLogger(DependencyFailurePropagator::class.java.name)

    override fun onLifecycleEvent(event: LifecycleEvent) {
        if (event.type != LifecycleEventType.FAILED) return
        propagate(event.container, event.cause)
    }

    private fun propagate(failedContainer: ModuleContainerView, cause: Throwable?) {
        val allContainers = containersProvider()
        val dependents = orchestrator.findDependents(
            targetId = failedContainer.module.id,
            allContainers = allContainers
        )

        if (dependents.isEmpty()) return

        log.warning(
            "Module '${failedContainer.module.id}' failed. " +
                "Propagating failure to ${dependents.size} dependent(s): " +
                dependents.map { it.module.id }
        )

        val activeDepContainers = dependents
            .filterIsInstance<ModuleContainer>()
            .filter { it.isActive() }

        val disableOrder = try {
            orchestrator.sortForDisable(activeDepContainers)
        } catch (e: CircularDependencyException) {
            log.warning("Could not sort dependents for disable: ${e.message}. Using original order.")
            activeDepContainers
        }

        for (container in disableOrder) {
            if (container !is ModuleContainer) continue
            val prevState = container.state
            try {
                val result = lifecycleManager.disable(container)
                if (result.isSuccess) {
                    eventBus.publish(
                        LifecycleEvent(
                            container = container,
                            type = LifecycleEventType.DEPENDENCY_FAILED,
                            previousState = prevState,
                            currentState = container.state,
                            cause = cause
                        )
                    )
                    log.warning("Force-disabled '${container.module.id}' due to dependency failure on '${failedContainer.module.id}'")
                }
            } catch (e: Exception) {
                log.warning("Failed to disable '${container.module.id}' during propagation: ${e.message}")
            }
        }
    }
}
