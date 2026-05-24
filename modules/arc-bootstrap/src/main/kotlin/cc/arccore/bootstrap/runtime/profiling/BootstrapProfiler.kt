package cc.arccore.bootstrap.runtime.profiling

import cc.arccore.bootstrap.runtime.BootstrapPhase

interface BootstrapProfiler {

    fun startBootstrap(moduleId: String)

    fun startPhase(moduleId: String, phase: BootstrapPhase)

    fun endPhase(moduleId: String, phase: BootstrapPhase, success: Boolean, notes: List<String> = emptyList())

    fun endBootstrap(moduleId: String): BootstrapProfilingData

    fun getProfilingData(moduleId: String): BootstrapProfilingData?
}
