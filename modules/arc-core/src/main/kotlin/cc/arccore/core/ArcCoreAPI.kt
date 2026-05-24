package cc.arccore.core

import cc.arccore.api.ArcAPI
import cc.arccore.api.ArcPlugin
import cc.arccore.api.command.CommandManager
import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.di.Container
import cc.arccore.api.event.EventManager
import cc.arccore.api.module.ModuleManager
import cc.arccore.api.registry.Registry
import cc.arccore.api.service.ServiceRegistry
import cc.arccore.command.CommandServiceImpl
import cc.arccore.command.DefaultCommandRegistry
import cc.arccore.core.module.ArcCoreModuleManager
import cc.arccore.core.registry.DefaultRegistry
import cc.arccore.di.ContainerImpl
import cc.arccore.di.DefaultServiceRegistry
import cc.arccore.event.EventServiceImpl

class ArcCoreAPI(
    override val plugin: ArcPlugin
) : ArcAPI {
    override val registry: Registry = DefaultRegistry()
    override lateinit var moduleManager: ModuleManager
    override val commandManager: CommandManager = CommandServiceImpl()
    override val eventManager: EventManager = EventServiceImpl()
    override val container: Container = ContainerImpl()
    override val serviceRegistry: ServiceRegistry = DefaultServiceRegistry()
    override val commandRegistry: CommandRegistry = DefaultCommandRegistry()
}
