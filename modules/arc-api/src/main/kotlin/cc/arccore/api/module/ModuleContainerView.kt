package cc.arccore.api.module

interface ModuleContainerView {

    val module: ArcModuleAPI

    val state: ModuleState

    val failureCause: Throwable?

    val context: ModuleContext?

    val description: ModuleDescription
        get() = module.description

    fun isLoaded(): Boolean =
        state == ModuleState.LOADED || state == ModuleState.ENABLED || state == ModuleState.DISABLED

    fun isActive(): Boolean = state == ModuleState.ENABLED

    fun isTerminal(): Boolean = state == ModuleState.FAILED || state == ModuleState.UNLOADED
}
