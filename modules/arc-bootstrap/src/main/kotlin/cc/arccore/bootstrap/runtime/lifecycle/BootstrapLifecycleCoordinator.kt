package cc.arccore.bootstrap.runtime.lifecycle

import cc.arccore.bootstrap.runtime.state.BootstrapResult
import java.util.logging.Logger

/**
 * Coordinates the transition from a completed bootstrap to the module enable phase.
 *
 * Because arc-bootstrap must not directly depend on arc-runtime's concrete lifecycle
 * types, enabling is delegated to a [ModuleEnableTrigger] — an abstraction that
 * arc-core or arc-runtime provides at wiring time.
 */
class BootstrapLifecycleCoordinator(
    private val enableTrigger: ModuleEnableTrigger,
    private val log: Logger = Logger.getLogger(BootstrapLifecycleCoordinator::class.java.name)
) {

    /**
     * Invoked when bootstrap completes for a module.
     * Triggers the enable phase only on [BootstrapResult.Success].
     */
    fun onBootstrapComplete(result: BootstrapResult) {
        when (result) {
            is BootstrapResult.Success -> {
                log.fine("[ARCCore] Bootstrap succeeded for '${result.moduleId}' — triggering enable")
                try {
                    enableTrigger.triggerEnable(result.moduleId)
                } catch (e: Exception) {
                    log.warning(
                        "[ARCCore] Enable trigger failed for '${result.moduleId}': ${e.message}"
                    )
                }
            }
            is BootstrapResult.Failure -> {
                log.warning(
                    "[ARCCore] Bootstrap failed for '${result.moduleId}' at phase ${result.failedPhase}: " +
                        "${result.cause.message} — module will not be enabled"
                )
                try {
                    enableTrigger.triggerFailure(result.moduleId, result.cause)
                } catch (e: Exception) {
                    log.warning(
                        "[ARCCore] Failure trigger threw for '${result.moduleId}': ${e.message}"
                    )
                }
            }
            is BootstrapResult.Skipped -> {
                log.fine("[ARCCore] Bootstrap skipped for '${result.moduleId}': ${result.reason}")
            }
        }
    }

    /**
     * Invoked when all modules have completed bootstrap.
     */
    fun onAllBootstrapComplete(results: List<BootstrapResult>) {
        val succeeded = results.count { it.isSuccess }
        val failed = results.count { it.isFailure }
        val skipped = results.count { it.isSkipped }
        log.info(
            "[ARCCore] All bootstrap complete: $succeeded succeeded, $failed failed, $skipped skipped"
        )
        try {
            enableTrigger.onAllComplete(results.map { it.moduleId })
        } catch (e: Exception) {
            log.warning("[ARCCore] onAllComplete trigger threw: ${e.message}")
        }
    }
}

/**
 * Abstraction for triggering module lifecycle transitions after bootstrap.
 * Implemented by arc-core or arc-runtime to avoid direct dependency.
 */
interface ModuleEnableTrigger {
    fun triggerEnable(moduleId: String)
    fun triggerFailure(moduleId: String, cause: Throwable)
    fun onAllComplete(moduleIds: List<String>) {}
}
