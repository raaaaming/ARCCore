package cc.arccore.runtime.reload

import cc.arccore.api.module.ModuleState
import cc.arccore.api.module.reload.ModuleReloadHint
import cc.arccore.api.module.reload.ModuleReloadOutcome
import cc.arccore.api.module.reload.ReloadResult
import cc.arccore.loader.loader.ModuleLoadResult
import cc.arccore.runtime.dependency.DependencyGraph
import cc.arccore.runtime.lifecycle.LifecycleResult
import cc.arccore.runtime.lifecycle.ModuleRuntime
import cc.arccore.runtime.reload.ordering.ReloadOrderCalculator
import cc.arccore.runtime.reload.rollback.ReloadRollbackManager
import cc.arccore.runtime.reload.validation.ReloadValidator
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class DefaultHotReloadManager(
    private val runtime: ModuleRuntime,
    private val jarPathProvider: (String) -> Path?
) : HotReloadManager {

    private val reloading = AtomicBoolean(false)
    private val rollbackManager = ReloadRollbackManager(runtime)
    private val log = Logger.getLogger(DefaultHotReloadManager::class.java.name)

    override fun isReloading(): Boolean = reloading.get()

    override fun reload(moduleId: String): ReloadResult {
        if (!reloading.compareAndSet(false, true)) {
            return ReloadResult.AlreadyReloading(moduleId)
        }
        return try {
            executeReload(moduleId)
        } finally {
            reloading.set(false)
        }
    }

    override fun reloadAll(moduleIds: List<String>): Map<String, ReloadResult> {
        return moduleIds.associateWith { reload(it) }
    }

    private fun executeReload(moduleId: String): ReloadResult {
        // 1. container 확인
        val container = runtime.getContainer(moduleId)
            ?: return ReloadResult.Failure(
                moduleId, "VALIDATING",
                IllegalArgumentException("Module '$moduleId' not found")
            )

        // 2. jar path 확인
        val jarPath = jarPathProvider(moduleId)
            ?: return ReloadResult.Failure(
                moduleId, "VALIDATING",
                IllegalStateException("No jar path for module '$moduleId'")
            )

        // 3. validation
        when (val failure = ReloadValidator.validate(container, jarPath)) {
            is ReloadValidator.ValidationFailure.InvalidState ->
                return ReloadResult.Failure(
                    moduleId, "VALIDATING",
                    IllegalStateException("Invalid state: ${failure.actual}")
                )
            is ReloadValidator.ValidationFailure.JarNotFound ->
                return ReloadResult.Failure(
                    moduleId, "VALIDATING",
                    IllegalStateException("Jar not found: ${failure.path}")
                )
            is ReloadValidator.ValidationFailure.ConditionalRejected ->
                return ReloadResult.Rejected(moduleId, failure.reason)
            null -> { /* pass */ }
        }

        // 4. dependency graph + disable order
        val allContainers = runtime.getContainers()
        val graph = DependencyGraph.build(allContainers)
        val disableOrder = ReloadOrderCalculator.calculateDisableOrder(moduleId, graph)
        val enableOrder = ReloadOrderCalculator.calculateEnableOrder(moduleId, disableOrder)

        // 5. context 생성
        val isStateful = container.module is ModuleReloadHint.StatefulReload
        val context = ReloadContext(
            targetModuleId = moduleId,
            jarPath = jarPath,
            affectedModuleIds = disableOrder,
            isStateful = isStateful
        )

        // 6. StatefulReload 상태 캡처
        context.phase = ReloadPhase.CAPTURING_STATE
        for (id in disableOrder) {
            val c = runtime.getContainer(id) ?: continue
            val module = c.module
            if (module is ModuleReloadHint.StatefulReload) {
                runCatching { context.capturedStates[id] = module.captureState() }
                    .onFailure { log.warning("captureState failed for '$id': ${it.message}") }
            }
        }

        // 7. dependent modules disable
        context.phase = ReloadPhase.DISABLING_DEPENDENTS
        val dependentsToDisable = disableOrder.filter { it != moduleId }
        for (depId in dependentsToDisable) {
            val depContainer = runtime.getContainer(depId) ?: continue
            if (depContainer.state != ModuleState.ENABLED) continue
            val result = runtime.disableModule(depId)
            if (result.isSuccess) {
                context.disabledModules.add(depId)
            } else {
                log.warning("Failed to disable dependent '$depId' before reload")
                val rollback = rollbackManager.rollback(context)
                return ReloadResult.Failure(
                    moduleId, "DISABLING_DEPENDENTS",
                    RuntimeException("Failed to disable dependent '$depId'"),
                    partialResults = buildPartialResults(rollback)
                )
            }
        }

        // 8. target disable
        context.phase = ReloadPhase.DISABLING_TARGET
        if (container.state == ModuleState.ENABLED) {
            val disableResult = runtime.disableModule(moduleId)
            if (disableResult.isSuccess) {
                context.disabledModules.add(moduleId)
            } else {
                val rollback = rollbackManager.rollback(context)
                return ReloadResult.Failure(
                    moduleId, "DISABLING_TARGET",
                    RuntimeException("Failed to disable target module '$moduleId'"),
                    partialResults = buildPartialResults(rollback)
                )
            }
        }

        // 9. target unload
        context.phase = ReloadPhase.UNLOADING_TARGET
        val unloadResult = runtime.unloadModule(moduleId)
        if (unloadResult.isFailure) {
            val rollback = rollbackManager.rollback(context)
            return ReloadResult.Failure(
                moduleId, "UNLOADING_TARGET",
                RuntimeException("Unload failed for '$moduleId'"),
                partialResults = buildPartialResults(rollback)
            )
        }

        // 10. new jar load
        context.phase = ReloadPhase.LOADING_NEW_JAR
        val loadResult = runtime.getModuleLoader().loadModule(jarPath)
        if (loadResult.isFailure) {
            val rollback = rollbackManager.rollback(context)
            val err = (loadResult as? ModuleLoadResult.Failure)?.error
                ?: RuntimeException("Module load failed for '$moduleId'")
            return ReloadResult.Failure(
                moduleId, "LOADING_NEW_JAR", err,
                partialResults = buildPartialResults(rollback)
            )
        }

        // 11. target enable
        context.phase = ReloadPhase.ENABLING_TARGET
        val enableResult = runtime.enableModule(moduleId)
        if (enableResult.isFailure) {
            val rollback = rollbackManager.rollback(context)
            val err = (enableResult as? LifecycleResult.Failure)?.error
                ?: RuntimeException("Enable failed for '$moduleId'")
            return ReloadResult.Failure(
                moduleId, "ENABLING_TARGET", err,
                partialResults = buildPartialResults(rollback)
            )
        }
        context.enabledModules.add(moduleId)

        // 12. dependents re-enable
        context.phase = ReloadPhase.ENABLING_DEPENDENTS
        val dependentsToEnable = enableOrder.filter { it != moduleId }
        val partialFailures = mutableListOf<ModuleReloadOutcome>()
        for (depId in dependentsToEnable) {
            val depContainer = runtime.getContainer(depId) ?: continue
            if (depContainer.state == ModuleState.ENABLED) continue
            val result = runtime.enableModule(depId)
            if (result.isSuccess) {
                context.enabledModules.add(depId)
            } else {
                log.warning("Failed to re-enable dependent '$depId' after reload")
                val err = (result as? LifecycleResult.Failure)?.error
                partialFailures.add(ModuleReloadOutcome(depId, false, err))
            }
        }

        // 13. StatefulReload 상태 복원
        context.phase = ReloadPhase.RESTORING_STATE
        for (id in enableOrder) {
            if (id !in context.enabledModules) continue
            val c = runtime.getContainer(id) ?: continue
            val module = c.module
            val captured = context.capturedStates[id] ?: continue
            if (module is ModuleReloadHint.StatefulReload) {
                runCatching { module.restoreState(captured) }
                    .onFailure { log.warning("restoreState failed for '$id': ${it.message}") }
            }
        }

        context.phase = ReloadPhase.COMPLETED
        val affectedModules = context.enabledModules.filter { it != moduleId }

        return if (partialFailures.isEmpty()) {
            log.info(
                "Hot reload complete: '$moduleId' (affected: ${affectedModules.size}) in ${context.elapsedMs}ms"
            )
            ReloadResult.Success(moduleId, affectedModules, context.elapsedMs)
        } else {
            ReloadResult.PartialSuccess(
                moduleId = moduleId,
                succeededModules = context.enabledModules,
                failedModules = partialFailures
            )
        }
    }

    private fun buildPartialResults(
        rollback: ReloadRollbackManager.RollbackResult
    ): List<ModuleReloadOutcome> {
        return rollback.failedRestores.map { (id, err) -> ModuleReloadOutcome(id, false, err) } +
            rollback.restoredModules.map { id -> ModuleReloadOutcome(id, true) }
    }
}
