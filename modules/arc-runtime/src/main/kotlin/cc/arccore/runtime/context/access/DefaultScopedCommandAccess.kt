package cc.arccore.runtime.context.access

import cc.arccore.api.command.ARCCommand
import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.module.CleanupScope
import cc.arccore.api.module.ModuleContainerView

class DefaultScopedCommandAccess(
    private val commandRegistry: CommandRegistry,
    private val owner: ModuleContainerView,
    private val cleanupScope: CleanupScope,
    private val bukkitBridge: BukkitCommandBridge? = null
) : ScopedCommandAccess {

    override fun register(command: ARCCommand, override: Boolean, autoCleanup: Boolean) {
        commandRegistry.register(command, owner, override)
        bukkitBridge?.register(command)
        if (autoCleanup) {
            val key = "cmd:${command.metadata.name}"
            cleanupScope.register(key, AutoCloseable { unregister(command.metadata.name) })
        }
    }

    override fun unregister(name: String) {
        if (commandRegistry.registeredBy(owner).contains(name)) {
            commandRegistry.unregister(name)
            bukkitBridge?.unregister(name)
        }
    }

    override fun unregisterAll(): Int {
        val names = commandRegistry.registeredBy(owner).toSet()
        names.forEach { bukkitBridge?.unregister(it) }
        return commandRegistry.unregisterAll(owner)
    }

    override fun getCommand(name: String): ARCCommand? = commandRegistry.getCommand(name)

    override fun registeredNames(): Set<String> = commandRegistry.registeredBy(owner)
}
