package cc.arccore.runtime.context

import cc.arccore.api.module.BaseModule
import cc.arccore.runtime.context.access.ScopedCommandAccess
import cc.arccore.runtime.context.access.ScopedListenerAccess
import cc.arccore.runtime.context.access.ScopedServiceAccess
import cc.arccore.runtime.context.scheduler.ModuleScheduler
import cc.arccore.runtime.resource.ResourceTracker

/**
 * BaseModule을 확장해 RuntimeModuleContext의 고급 기능(scheduler, services, commands, listeners)을
 * 직접 접근 가능하게 하는 편의 베이스 클래스.
 *
 * DefaultModuleContextFactory가 구성된 환경(ArcCorePlugin)에서만 올바르게 동작한다.
 * SimpleModuleContextFactory 환경(단위 테스트 등)에서는 runtimeContext 접근 시 에러가 발생한다.
 *
 * 사용 예:
 *   class EconomyModule : BaseRuntimeModule() {
 *       override fun onEnable() {
 *           scheduler.runAsync { fetchPrices() }
 *           val economy = services.require(EconomyService::class)
 *       }
 *   }
 */
abstract class BaseRuntimeModule : BaseModule() {

    protected val runtimeContext: RuntimeModuleContext
        get() = context as? RuntimeModuleContext
            ?: error(
                "Module '$id' context is not a RuntimeModuleContext. " +
                    "Ensure DefaultModuleContextFactory is configured in ModuleRuntime."
            )

    protected val scheduler: ModuleScheduler
        get() = runtimeContext.scheduler

    protected val services: ScopedServiceAccess
        get() = runtimeContext.services

    protected val commands: ScopedCommandAccess
        get() = runtimeContext.commands

    protected val listeners: ScopedListenerAccess
        get() = runtimeContext.listeners

    protected val runtime: RuntimeFacade
        get() = runtimeContext.runtime

    protected val resourceTracker: ResourceTracker
        get() = runtimeContext.runtime.resources()
}
