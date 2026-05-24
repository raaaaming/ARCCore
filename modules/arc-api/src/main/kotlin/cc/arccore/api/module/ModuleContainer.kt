package cc.arccore.api.module

import cc.arccore.api.exception.ModuleStateException

@Deprecated(
    message = "ModuleContainer will be replaced by DefaultModuleContainer in arc-runtime for thread safety. " +
        "Use ModuleContainerView for read-only access.",
    level = DeprecationLevel.WARNING
)
class ModuleContainer(
    override val module: ArcModuleAPI
) : ModuleContainerView {

    override var state: ModuleState = ModuleState.CREATED
        private set

    override var failureCause: Throwable? = null
        private set

    override val description: ModuleDescription
        get() = module.description

    override var context: ModuleContext? = null
        private set

    fun transitionTo(newState: ModuleState, error: Throwable? = null) {
        if (!state.canTransitionTo(newState)) {
            throw ModuleStateException(
                "Cannot transition module '${module.id}' from $state to $newState"
            )
        }
        if (newState == ModuleState.FAILED) {
            failureCause = error
        }
        state = newState
    }

    fun transitionToLoad(context: ModuleContext) {
        transitionTo(ModuleState.LOADED)
        this.context = context
    }

    fun transitionToFailed(error: Throwable? = null) {
        transitionTo(ModuleState.FAILED, error)
    }

    fun transitionToUnloaded() {
        transitionTo(ModuleState.UNLOADED)
        this.context = null
    }

    override fun toString(): String =
        "ModuleContainer(module=${module.id}, state=$state)"
}
