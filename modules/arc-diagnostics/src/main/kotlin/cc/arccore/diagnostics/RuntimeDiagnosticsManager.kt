package cc.arccore.diagnostics

import cc.arccore.diagnostics.model.RuntimeMetrics
import cc.arccore.diagnostics.model.RuntimeSnapshot
import cc.arccore.diagnostics.reporting.DiagnosticsReporter
import cc.arccore.diagnostics.tracking.ModuleDiagnosticsEntry

interface RuntimeDiagnosticsManager {
    fun takeSnapshot(): RuntimeSnapshot
    fun collectMetrics(): RuntimeMetrics
    fun getModuleEntry(moduleId: String): ModuleDiagnosticsEntry?
    fun getReporter(): DiagnosticsReporter
    fun shutdown()
}
