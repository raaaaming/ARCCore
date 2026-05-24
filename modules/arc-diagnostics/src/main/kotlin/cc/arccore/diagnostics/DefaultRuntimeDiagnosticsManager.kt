package cc.arccore.diagnostics

import cc.arccore.api.lifecycle.LifecycleObserver
import cc.arccore.diagnostics.lifecycle.DiagnosticsLifecycleObserver
import cc.arccore.diagnostics.metrics.RuntimeMetricsCollector
import cc.arccore.diagnostics.model.RuntimeMetrics
import cc.arccore.diagnostics.model.RuntimeSnapshot
import cc.arccore.diagnostics.reporting.DiagnosticsReporter
import cc.arccore.diagnostics.snapshot.SnapshotBuilder
import cc.arccore.diagnostics.tracking.DiagnosticsRegistry
import cc.arccore.diagnostics.tracking.ModuleDiagnosticsEntry
import cc.arccore.runtime.lifecycle.ModuleRuntime

class DefaultRuntimeDiagnosticsManager(
    private val runtime: ModuleRuntime
) : RuntimeDiagnosticsManager {

    private val registry = DiagnosticsRegistry()
    private val snapshotBuilder = SnapshotBuilder(runtime, registry)
    private val metricsCollector = RuntimeMetricsCollector(registry)
    private val reporter = DiagnosticsReporter()
    val lifecycleObserver: LifecycleObserver = DiagnosticsLifecycleObserver(registry)

    override fun takeSnapshot(): RuntimeSnapshot = snapshotBuilder.buildSnapshot()

    override fun collectMetrics(): RuntimeMetrics {
        val snapshot = takeSnapshot()
        return metricsCollector.collect(snapshot)
    }

    override fun getModuleEntry(moduleId: String): ModuleDiagnosticsEntry? =
        registry.getEntry(moduleId)

    override fun getReporter(): DiagnosticsReporter = reporter

    override fun shutdown() {
        runtime.removeLifecycleObserver(lifecycleObserver)
    }
}
