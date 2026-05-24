package cc.arccore.api

import cc.arccore.api.command.CommandManager
import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.di.Container
import cc.arccore.api.event.EventManager
import cc.arccore.api.module.ModuleManager
import cc.arccore.api.registry.Registry
import cc.arccore.api.service.ServiceRegistry

interface ArcAPI {
    val plugin: ArcPlugin
    val registry: Registry
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val eventManager: EventManager
    val container: Container
    val serviceRegistry: ServiceRegistry
    val commandRegistry: CommandRegistry
}
