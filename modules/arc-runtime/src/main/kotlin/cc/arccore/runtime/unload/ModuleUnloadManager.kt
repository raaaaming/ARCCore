package cc.arccore.runtime.unload

import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.api.module.ModuleContainer
import cc.arccore.loader.ModuleClassLoader
import cc.arccore.api.service.ServiceRegistry
import cc.arccore.runtime.unload.cleanup.CleanupAware
import cc.arccore.runtime.unload.cleanup.ClassLoaderCleanupStep
import cc.arccore.runtime.unload.cleanup.ClassLoaderReleaseStep
import cc.arccore.runtime.unload.cleanup.CommandRegistryCleanupStep
import cc.arccore.runtime.unload.cleanup.ListenerCleanupStep
import cc.arccore.runtime.unload.cleanup.PreUnloadCleanupStep
import cc.arccore.runtime.unload.cleanup.PrioritizedCleanupStep
import cc.arccore.runtime.unload.cleanup.RegistryCleanupStep
import cc.arccore.runtime.unload.cleanup.SchedulerCleanupStep
import cc.arccore.runtime.unload.cleanup.ServiceCleanupStep
import cc.arccore.runtime.unload.cleanup.ServiceRegistryCleanupStep
import cc.arccore.runtime.unload.leak.LeakDetector
import cc.arccore.runtime.unload.leak.ModuleClassLoaderTracker
import java.util.logging.Logger

interface ModuleUnloadManager {

    fun executeTeardown(container: ModuleContainer): TeardownResult

    fun getCleanupPipeline(): ModuleCleanupPipeline

    fun getLeakDetector(): LeakDetector
}

class DefaultModuleUnloadManager(
    private val unregisterFromRegistry: (String) -> Unit,
    /**
     * 글로벌 ServiceRegistry. null이면 ServiceRegistryCleanupStep이 파이프라인에 설치되지 않는다.
     * 프로덕션에서는 [cc.arccore.api.ArcAPI.serviceRegistry]를 반드시 주입해야 한다.
     */
    private val serviceRegistry: ServiceRegistry? = null,
    private val commandRegistry: CommandRegistry? = null,
    private val classLoaderTracker: ModuleClassLoaderTracker = ModuleClassLoaderTracker()
) : ModuleUnloadManager, AutoCloseable {

    private val log = Logger.getLogger(DefaultModuleUnloadManager::class.java.name)
    private val pipeline = ModuleCleanupPipeline()

    init {
        for (step in defaultCleanupSteps()) {
            pipeline.install(step)
        }
    }

    override fun executeTeardown(container: ModuleContainer): TeardownResult {
        val moduleId = container.module.id
        log.info("Executing teardown for module '$moduleId'")

        val classLoader = resolveClassLoader(container)
        val ctx = CleanupContext.create(container, classLoader)
        val cleanupReport = pipeline.execute(ctx)

        val leakWarnings = LeakDetector.detectPotentialLeaks(container)
        for (warning in leakWarnings) {
            log.warning("Leak detected for module '$moduleId': [${warning.severity}] ${warning.message}")
        }

        val module = container.module
        if (module is CleanupAware) {
            try {
                module.onCleanupComplete()
            } catch (e: Exception) {
                log.warning("Module '$moduleId' onCleanupComplete() threw: ${e.message}")
            }
        }

        return TeardownResult(moduleId, cleanupReport, leakWarnings)
    }

    override fun getCleanupPipeline(): ModuleCleanupPipeline = pipeline

    override fun getLeakDetector(): LeakDetector = LeakDetector

    private fun resolveClassLoader(container: ModuleContainer): ModuleClassLoader? {
        val context = container.context
        return if (context is ClassLoaderHolder) {
            context.provideClassLoader() as? ModuleClassLoader
        } else {
            null
        }
    }

    override fun close() {
        classLoaderTracker.close()
    }

    private fun defaultCleanupSteps(): List<PrioritizedCleanupStep> {
        val steps = mutableListOf(
            PreUnloadCleanupStep(),
            SchedulerCleanupStep(),
            ListenerCleanupStep(),
            ServiceCleanupStep(),
            RegistryCleanupStep(unregisterFromRegistry),
            ClassLoaderCleanupStep(),
            ClassLoaderReleaseStep(classLoaderTracker)
        )
        serviceRegistry?.let { steps.add(ServiceRegistryCleanupStep(it)) }
        commandRegistry?.let { steps.add(CommandRegistryCleanupStep(it)) }
        return steps
    }
}

data class TeardownResult(
    val moduleId: String,
    val cleanupReport: ModuleCleanupPipeline.CleanupReport,
    val leakWarnings: List<LeakDetector.LeakWarning>
) {
    val success: Boolean get() = cleanupReport.allSuccessful
    val hasLeaks: Boolean get() = leakWarnings.isNotEmpty()
}
