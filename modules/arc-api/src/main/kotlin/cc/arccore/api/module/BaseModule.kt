package cc.arccore.api.module

import cc.arccore.api.exception.ModuleStateException

abstract class BaseModule : ArcModuleAPI {

    internal var _context: ModuleContext? = null

    protected val context: ModuleContext
        get() = _context ?: error(
            "Module '$id' context accessed before onLoad(). " +
                "Ensure onLoad(context) has been invoked."
        )

    protected val logger: ModuleLogger
        get() = context.logger

    protected val dataFolder: java.nio.file.Path
        get() = context.dataFolder

    final override val id: String by lazy {
        _context?.description?.id ?: error("Module id accessed before onLoad()")
    }

    final override val description: ModuleDescription
        get() = context.description

    override fun onLoad(context: ModuleContext) {
        if (!context.state.canTransitionTo(ModuleState.LOADED)) {
            throw ModuleStateException(
                "Module '${context.description.id}' cannot load from state ${context.state}"
            )
        }
        _context = context
    }

    override fun onEnable() {
        if (!context.state.canTransitionTo(ModuleState.ENABLED)) {
            throw ModuleStateException(
                "Module '$id' cannot enable from state ${context.state}"
            )
        }
    }

    override fun onDisable() {
        if (!context.state.canTransitionTo(ModuleState.DISABLED)) {
            throw ModuleStateException(
                "Module '$id' cannot disable from state ${context.state}"
            )
        }
    }

    override fun onUnload() {
        if (!context.state.canTransitionTo(ModuleState.UNLOADED)) {
            throw ModuleStateException(
                "Module '$id' cannot unload from state ${context.state}"
            )
        }
    }
}
