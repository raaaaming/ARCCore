package cc.arccore.runtime.lifecycle

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.module.ModuleContainer
import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.MutableModuleContext
import cc.arccore.runtime.dependency.DependencyViolation
import cc.arccore.runtime.dependency.GraphIntegration
import cc.arccore.runtime.dependency.GraphValidationException
import cc.arccore.runtime.dependency.GraphValidationResult
import cc.arccore.runtime.lifecycle.exception.ModuleDisableException
import cc.arccore.runtime.lifecycle.exception.ModuleEnableException
import cc.arccore.runtime.lifecycle.exception.ModuleUnloadException
import cc.arccore.runtime.lifecycle.validation.LifecycleValidator
import cc.arccore.runtime.resource.DefaultResourceTracker
import cc.arccore.runtime.resource.ResourceTracker
import cc.arccore.runtime.unload.DefaultModuleUnloadManager
import cc.arccore.runtime.unload.ModuleUnloadManager
import cc.arccore.runtime.unload.ModuleResourceTracker
import cc.arccore.runtime.unload.ModuleTaskTracker
import java.util.logging.Logger

class DefaultModuleLifecycleManager(
    private val cleanupRegistry: ModuleCleanupRegistry = ModuleCleanupRegistry(),
    private val unloadManager: ModuleUnloadManager? = null,
    private val unregisterFromRegistry: (String) -> Unit = {},
    private val eventBus: LifecycleEventBus = LifecycleEventBus(),
    private val orchestrator: LifecycleOrchestrator = LifecycleOrchestrator(),
    private val graphIntegration: GraphIntegration? = null
) : ModuleLifecycleManager {

    private val log = Logger.getLogger(DefaultModuleLifecycleManager::class.java.name)
    private val internalUnloadManager: ModuleUnloadManager =
        unloadManager ?: DefaultModuleUnloadManager(unregisterFromRegistry)

    private val taskTrackers = java.util.concurrent.ConcurrentHashMap<String, ModuleTaskTracker>()
    private val resourceTrackers = java.util.concurrent.ConcurrentHashMap<String, ModuleResourceTracker>()
    private val ownershipTracker = DefaultResourceTracker()

    override fun enable(container: ModuleContainer): LifecycleResult {
        LifecycleValidator.validateCanEnable(container)
        log.info("Enabling module '${container.module.id}'")

        val prevState = container.state
        syncContextState(container, ModuleState.ENABLING)
        container.transitionTo(ModuleState.ENABLING)

        return try {
            container.module.onEnable()
            syncContextState(container, ModuleState.ENABLED)
            container.transitionTo(ModuleState.ENABLED)
            log.info("Enabled module '${container.module.id}'")
            eventBus.publish(LifecycleEvent(container, LifecycleEventType.ENABLED, prevState, ModuleState.ENABLED))
            LifecycleResult.Success(container)
        } catch (e: Exception) {
            log.severe("Failed to enable module '${container.module.id}': ${e.message}")
            performRollback(container, ModuleState.ENABLING, e)
            if (container.state == ModuleState.FAILED) {
                eventBus.publish(LifecycleEvent(container, LifecycleEventType.FAILED, prevState, ModuleState.FAILED, e))
            }
            LifecycleResult.Failure(
                container = container,
                error = ModuleEnableException("Failed to enable module '${container.module.id}'", e),
                rollbackSuccess = container.state != ModuleState.FAILED
            )
        }
    }

    override fun disable(container: ModuleContainer): LifecycleResult {
        LifecycleValidator.validateCanDisable(container)
        log.info("Disabling module '${container.module.id}'")

        val prevState = container.state
        syncContextState(container, ModuleState.DISABLING)
        container.transitionTo(ModuleState.DISABLING)

        return try {
            container.module.onDisable()
            syncContextState(container, ModuleState.DISABLED)
            container.transitionTo(ModuleState.DISABLED)
            log.info("Disabled module '${container.module.id}'")
            eventBus.publish(LifecycleEvent(container, LifecycleEventType.DISABLED, prevState, ModuleState.DISABLED))
            LifecycleResult.Success(container)
        } catch (e: Exception) {
            log.severe("Failed to disable module '${container.module.id}': ${e.message}")
            performRollback(container, ModuleState.DISABLING, e)
            if (container.state == ModuleState.FAILED) {
                eventBus.publish(LifecycleEvent(container, LifecycleEventType.FAILED, prevState, ModuleState.FAILED, e))
            }
            LifecycleResult.Failure(
                container = container,
                error = ModuleDisableException("Failed to disable module '${container.module.id}'", e),
                rollbackSuccess = container.state != ModuleState.FAILED
            )
        }
    }

    override fun unload(container: ModuleContainer): LifecycleResult {
        LifecycleValidator.validateCanUnload(container)
        log.info("Unloading module '${container.module.id}'")

        val prevState = container.state
        syncContextState(container, ModuleState.UNLOADING)
        container.transitionTo(ModuleState.UNLOADING)

        try {
            container.module.onUnload()
        } catch (e: Exception) {
            log.severe("Module '${container.module.id}' threw exception during onUnload(): ${e.message}")
        }

        cleanupRegistry.runCleanup(container)

        val teardownResult = internalUnloadManager.executeTeardown(container)

        if (!teardownResult.success) {
            for (report in teardownResult.cleanupReport.failedSteps) {
                log.warning("Cleanup step '${report.stepName}' failed for '${container.module.id}': ${report.error?.message}")
            }
        }

        if (teardownResult.hasLeaks) {
            for (warning in teardownResult.leakWarnings) {
                log.warning("Leak: [${warning.severity}] ${warning.message} (source: ${warning.source})")
            }
        }

        cleanupTrackers(container.module.id)

        return try {
            syncContextState(container, ModuleState.UNLOADED)
            container.transitionToUnloaded()
            log.info("Unloaded module '${container.module.id}'")
            eventBus.publish(LifecycleEvent(container, LifecycleEventType.UNLOADED, prevState, ModuleState.UNLOADED))
            LifecycleResult.Success(container)
        } catch (e: Exception) {
            log.severe("Failed to finalize unload for module '${container.module.id}': ${e.message}")
            performRollback(container, ModuleState.UNLOADING, e)
            if (container.state == ModuleState.FAILED) {
                eventBus.publish(LifecycleEvent(container, LifecycleEventType.FAILED, prevState, ModuleState.FAILED, e))
            }
            LifecycleResult.Failure(
                container = container,
                error = ModuleUnloadException("Failed to unload module '${container.module.id}'", e),
                rollbackSuccess = container.state != ModuleState.FAILED
            )
        }
    }

    override fun enableAll(containers: List<ModuleContainer>): List<LifecycleResult> {
        val containerById = containers.associateBy { it.module.id }
        val ordered: List<ModuleContainer> = if (graphIntegration != null) {
            val result = graphIntegration.validateOnly(containers)
            when (result) {
                is GraphValidationResult.Valid -> {
                    result.warnings.forEach { log.warning("Dependency warning: $it") }
                    result.sortedForEnable.mapNotNull { containerById[it.module.id] }
                }
                is GraphValidationResult.Invalid -> {
                    val errors = result.violations.filter { it.severity == DependencyViolation.Severity.ERROR }
                    errors.forEach { log.severe("Dependency error: $it") }
                    if (errors.isNotEmpty()) {
                        return containers.map {
                            LifecycleResult.Failure(it, GraphValidationException(errors), false)
                        }
                    }
                    try {
                        orchestrator.sortForEnable(containers).mapNotNull { containerById[it.module.id] }
                    } catch (e: CircularDependencyException) {
                        containers
                    }
                }
            }
        } else {
            try {
                orchestrator.sortForEnable(containers).mapNotNull { containerById[it.module.id] }
            } catch (e: CircularDependencyException) {
                log.warning("Circular dependency detected, falling back to original order: ${e.message}")
                containers
            }
        }
        return ordered.map { enable(it) }
    }

    override fun disableAll(containers: List<ModuleContainer>): List<LifecycleResult> {
        val containerById = containers.associateBy { it.module.id }
        val ordered: List<ModuleContainer> = if (graphIntegration != null) {
            graphIntegration.sortForDisable(containers).mapNotNull { containerById[it.module.id] }
        } else {
            try {
                orchestrator.sortForDisable(containers).mapNotNull { containerById[it.module.id] }
            } catch (e: CircularDependencyException) {
                log.warning("Circular dependency detected, falling back to reversed order: ${e.message}")
                containers.reversed()
            }
        }
        return ordered.map { disable(it) }
    }

    override fun unloadAll(containers: List<ModuleContainer>): List<LifecycleResult> {
        val containerById = containers.associateBy { it.module.id }
        val ordered: List<ModuleContainer> = if (graphIntegration != null) {
            graphIntegration.sortForDisable(containers).mapNotNull { containerById[it.module.id] }
        } else {
            try {
                orchestrator.sortForDisable(containers).mapNotNull { containerById[it.module.id] }
            } catch (e: CircularDependencyException) {
                log.warning("Circular dependency detected, falling back to reversed order: ${e.message}")
                containers.reversed()
            }
        }
        return ordered.map { unload(it) }
    }

    override fun getCleanupRegistry(): ModuleCleanupRegistry = cleanupRegistry

    fun getEventBus(): LifecycleEventBus = eventBus

    fun publishLoaded(container: ModuleContainer, prevState: cc.arccore.api.module.ModuleState) {
        eventBus.publish(LifecycleEvent(container, LifecycleEventType.LOADED, prevState, ModuleState.LOADED))
    }

    fun getTaskTracker(moduleId: String): ModuleTaskTracker {
        return taskTrackers.computeIfAbsent(moduleId) { ModuleTaskTracker(it) }
    }

    fun getResourceTracker(moduleId: String): ModuleResourceTracker {
        return resourceTrackers.computeIfAbsent(moduleId) { ModuleResourceTracker(it) }
    }

    fun getOwnershipTracker(): ResourceTracker = ownershipTracker

    fun getUnloadManager(): ModuleUnloadManager = internalUnloadManager

    private fun cleanupTrackers(moduleId: String) {
        taskTrackers.remove(moduleId)?.cancelAllTasks()
        resourceTrackers.remove(moduleId)?.releaseAll()
        ownershipTracker.releaseModule(moduleId)
    }

    private fun performRollback(container: ModuleContainer, failedState: ModuleState, cause: Throwable) {
        val target = rollbackTarget(failedState)
        if (target != null && container.state.canTransitionTo(target)) {
            log.warning("Rolling back module '${container.module.id}' from $failedState to $target")
            try {
                syncContextState(container, target)
                container.transitionTo(target)
            } catch (e: Exception) {
                log.severe("Rollback failed for module '${container.module.id}': ${e.message}")
                try {
                    syncContextState(container, ModuleState.FAILED)
                    container.transitionToFailed(cause)
                } catch (_: Exception) {
                    log.severe("Failed to mark module '${container.module.id}' as FAILED")
                }
            }
        } else {
            log.warning("No rollback path from $failedState, marking module '${container.module.id}' as FAILED")
            try {
                syncContextState(container, ModuleState.FAILED)
                container.transitionToFailed(cause)
            } catch (_: Exception) {
            }
        }
    }

    private fun rollbackTarget(failedState: ModuleState): ModuleState? {
        return when (failedState) {
            ModuleState.ENABLING -> ModuleState.LOADED
            ModuleState.DISABLING -> ModuleState.ENABLED
            ModuleState.UNLOADING -> ModuleState.DISABLED
            else -> null
        }
    }

    private fun syncContextState(container: ModuleContainer, newState: ModuleState) {
        val context = container.context
        if (context is MutableModuleContext) {
            context.updateState(newState)
        }
    }
}
