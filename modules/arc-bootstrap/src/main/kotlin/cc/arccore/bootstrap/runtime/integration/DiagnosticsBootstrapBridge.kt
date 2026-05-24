package cc.arccore.bootstrap.runtime.integration

import cc.arccore.bootstrap.runtime.metrics.BootstrapMetrics
import cc.arccore.bootstrap.runtime.state.BootstrapResult
import java.util.logging.Logger

/**
 * Optional bridge to arc-diagnostics.
 *
 * arc-diagnostics is a compileOnly / optional dependency.
 * All calls are wrapped in try-catch so arc-bootstrap compiles and runs
 * even when arc-diagnostics is absent from the classpath.
 */
class DiagnosticsBootstrapBridge {

    companion object {
        private const val DIAGNOSTICS_REGISTRY_CLASS = "cc.arccore.diagnostics.RuntimeSnapshotRegistry"
        private const val METRICS_RECORDER_CLASS = "cc.arccore.diagnostics.metrics.MetricsRecorder"
    }

    private val log = Logger.getLogger(DiagnosticsBootstrapBridge::class.java.name)

    private val available: Boolean by lazy {
        try {
            Class.forName(DIAGNOSTICS_REGISTRY_CLASS)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun isAvailable(): Boolean = available

    /**
     * Records a bootstrap result into the diagnostics system if available.
     */
    fun recordBootstrapResult(result: BootstrapResult) {
        if (!available) return
        try {
            recordResultInternal(result)
        } catch (e: Exception) {
            log.fine("[ARCCore] DiagnosticsBootstrapBridge.recordBootstrapResult failed silently: ${e.message}")
        }
    }

    /**
     * Records aggregate bootstrap metrics into the diagnostics system if available.
     */
    fun recordBootstrapMetrics(metrics: BootstrapMetrics) {
        if (!available) return
        try {
            recordMetricsInternal(metrics)
        } catch (e: Exception) {
            log.fine("[ARCCore] DiagnosticsBootstrapBridge.recordBootstrapMetrics failed silently: ${e.message}")
        }
    }

    /**
     * Signals to diagnostics that bootstrap phase has completed.
     */
    fun signalBootstrapComplete(moduleIds: List<String>) {
        if (!available) return
        try {
            signalCompleteInternal(moduleIds)
        } catch (e: Exception) {
            log.fine("[ARCCore] DiagnosticsBootstrapBridge.signalBootstrapComplete failed silently: ${e.message}")
        }
    }

    // Internal reflection-based calls — only reached when diagnostics is available

    private fun recordResultInternal(result: BootstrapResult) {
        val registryClass = Class.forName(DIAGNOSTICS_REGISTRY_CLASS)
        val instanceField = try {
            registryClass.getDeclaredField("INSTANCE")
        } catch (_: NoSuchFieldException) {
            return
        }
        instanceField.isAccessible = true
        val instance = instanceField.get(null) ?: return

        val method = try {
            registryClass.getDeclaredMethod("onBootstrapResult", String::class.java, Boolean::class.java)
        } catch (_: NoSuchMethodException) {
            return
        }
        method.invoke(instance, result.moduleId, result.isSuccess)
    }

    private fun recordMetricsInternal(metrics: BootstrapMetrics) {
        // No-op stub: actual implementation depends on diagnostics API shape.
        // Full wiring is performed by arc-core which has compile-time access to arc-diagnostics.
        log.fine(
            "[ARCCore] Bootstrap metrics: ${metrics.totalModulesSucceeded}/${metrics.totalModulesAttempted} succeeded, " +
                "total=${String.format("%.1f", metrics.totalBootstrapDurationMs)}ms"
        )
    }

    private fun signalCompleteInternal(moduleIds: List<String>) {
        log.fine("[ARCCore] Bootstrap complete for modules: $moduleIds")
    }
}
