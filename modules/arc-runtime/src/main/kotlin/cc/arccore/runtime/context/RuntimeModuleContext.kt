package cc.arccore.runtime.context

import cc.arccore.api.module.MutableModuleContext
import cc.arccore.runtime.context.access.ScopedCommandAccess
import cc.arccore.runtime.context.access.ScopedListenerAccess
import cc.arccore.runtime.context.access.ScopedServiceAccess
import cc.arccore.runtime.context.async.AsyncRuntimeAccess
import cc.arccore.runtime.context.scheduler.ModuleScheduler

interface RuntimeModuleContext : MutableModuleContext {
    val scheduler: ModuleScheduler
    val services: ScopedServiceAccess
    val commands: ScopedCommandAccess
    val listeners: ScopedListenerAccess
    val runtime: RuntimeFacade
    val asyncRuntime: AsyncRuntimeAccess?
    val isValid: Boolean
}
