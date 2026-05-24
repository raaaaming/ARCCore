package cc.arccore.runtime.context.lifecycle

import cc.arccore.api.module.ModuleContext
import cc.arccore.api.module.ModuleState
import cc.arccore.runtime.context.exception.InvalidModuleContextException
import cc.arccore.runtime.context.exception.LifecycleViolationException

interface LifecycleGuard {
    fun isContextValid(context: ModuleContext): Boolean
    fun isContextActive(context: ModuleContext): Boolean
    fun requireValid(context: ModuleContext)
    fun requireActive(context: ModuleContext)
    fun <R> guarded(context: ModuleContext, block: () -> R): R
}

class DefaultLifecycleGuard : LifecycleGuard {

    override fun isContextValid(context: ModuleContext): Boolean {
        if (context.cleanupScope.isClosed) return false
        return context.state != ModuleState.UNLOADED && context.state != ModuleState.FAILED
    }

    override fun isContextActive(context: ModuleContext): Boolean {
        return isContextValid(context) && context.state == ModuleState.ENABLED
    }

    override fun requireValid(context: ModuleContext) {
        if (!isContextValid(context)) {
            throw InvalidModuleContextException(
                "Module context is no longer valid: state=${context.state}, scopeClosed=${context.cleanupScope.isClosed}"
            )
        }
    }

    override fun requireActive(context: ModuleContext) {
        if (!isContextActive(context)) {
            throw LifecycleViolationException(
                "Module context is not active: state=${context.state}, scopeClosed=${context.cleanupScope.isClosed}"
            )
        }
    }

    override fun <R> guarded(context: ModuleContext, block: () -> R): R {
        requireValid(context)
        return block()
    }
}
