package cc.arccore.diagnostics.tracking

import java.util.concurrent.ConcurrentHashMap

class DiagnosticsRegistry {
    private val entries = ConcurrentHashMap<String, ModuleDiagnosticsEntry>()

    fun onModuleLoaded(moduleId: String) {
        entries.getOrPut(moduleId) { ModuleDiagnosticsEntry(moduleId) }.onLoaded()
    }

    fun onModuleEnabled(moduleId: String) {
        entries[moduleId]?.onEnabled()
    }

    fun onModuleUnloaded(moduleId: String) {
        entries[moduleId]?.onUnloaded()
    }

    fun onModuleFailed(moduleId: String) {
        entries[moduleId]?.onFailed()
    }

    fun getEntry(moduleId: String): ModuleDiagnosticsEntry? = entries[moduleId]

    fun allEntries(): Collection<ModuleDiagnosticsEntry> = entries.values

    fun removeEntry(moduleId: String) {
        entries.remove(moduleId)
    }
}
