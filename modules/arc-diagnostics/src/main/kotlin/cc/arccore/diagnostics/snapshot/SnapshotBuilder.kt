package cc.arccore.diagnostics.snapshot

import cc.arccore.api.module.ModuleContainerView
import cc.arccore.api.module.ModuleState
import cc.arccore.diagnostics.model.ModuleSnapshot
import cc.arccore.diagnostics.model.RuntimeSnapshot
import cc.arccore.diagnostics.tracking.DiagnosticsRegistry
import cc.arccore.runtime.context.RuntimeModuleContext
import cc.arccore.runtime.lifecycle.ModuleRuntime
import java.time.Instant

class SnapshotBuilder(
    private val runtime: ModuleRuntime,
    private val registry: DiagnosticsRegistry,
    private val startTimeMs: Long = System.currentTimeMillis()
) {

    fun buildSnapshot(): RuntimeSnapshot {
        val now = Instant.now()
        val containers = runtime.getContainers()
        val modules = containers.map { buildModuleSnapshot(it) }

        val rt = Runtime.getRuntime()
        val usedBytes = rt.totalMemory() - rt.freeMemory()
        val maxBytes = rt.maxMemory()
        val usedMb = usedBytes / (1024L * 1024L)
        val maxMb = maxBytes / (1024L * 1024L)
        val usedPercent = if (maxMb > 0) ((usedMb * 100) / maxMb).toInt() else 0

        return RuntimeSnapshot(
            timestamp = now,
            uptimeMs = System.currentTimeMillis() - startTimeMs,
            totalModules = modules.size,
            enabledModules = modules.count { it.state == ModuleState.ENABLED },
            disabledModules = modules.count { it.state == ModuleState.DISABLED },
            failedModules = modules.count { it.state == ModuleState.FAILED },
            modules = modules,
            totalActiveServices = modules.sumOf { it.activeServices.size },
            totalActiveCommands = modules.sumOf { it.activeCommands.size },
            totalActiveListeners = modules.sumOf { it.activeListeners.size },
            totalActiveAsyncTasks = modules.sumOf { it.activeAsyncTasks },
            memoryUsedMb = usedMb,
            memoryMaxMb = maxMb,
            memoryUsedPercent = usedPercent
        )
    }

    private fun buildModuleSnapshot(container: ModuleContainerView): ModuleSnapshot {
        val id = container.module.id
        val entry = registry.getEntry(id)
        val ctx = container.context as? RuntimeModuleContext

        val serviceTypes = ctx?.services?.registeredTypes()
            ?.mapNotNull { it.simpleName ?: it.qualifiedName }
            ?.toSet() ?: emptySet()

        val commandNames = ctx?.commands?.registeredNames() ?: emptySet()

        val listenerNames = ctx?.listeners?.registeredListeners()
            ?.mapNotNull { it::class.simpleName }
            ?.toSet() ?: emptySet()

        val asyncTasks = (ctx?.asyncRuntime?.activeTaskCount() ?: 0) +
            (ctx?.scheduler?.activeTaskCount() ?: 0)

        val classLoaderId = try {
            container.module::class.java.classLoader
                ?.let { "CL@${Integer.toHexString(System.identityHashCode(it))}" }
        } catch (e: Exception) { null }

        val deps = container.description.dependencies.map { it.id } +
            container.description.softDependencies.map { "${it.id}?" }

        return ModuleSnapshot(
            moduleId = id,
            moduleName = container.description.name,
            version = container.description.version.toString(),
            state = container.state,
            loadedAt = entry?.loadedAt,
            enabledAt = entry?.enabledAt,
            reloadGeneration = entry?.reloadGeneration ?: 0,
            classLoaderId = classLoaderId,
            activeServices = serviceTypes,
            activeCommands = commandNames,
            activeListeners = listenerNames,
            activeAsyncTasks = asyncTasks,
            dependencies = deps,
            failureCause = container.failureCause?.message
        )
    }
}
