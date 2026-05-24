package cc.arccore.api.command

import cc.arccore.api.command.exception.DuplicateCommandException
import cc.arccore.api.module.ModuleContainerView

interface CommandRegistry {

    @Throws(DuplicateCommandException::class)
    fun register(command: ARCCommand, owner: ModuleContainerView, override: Boolean = false)

    fun unregister(name: String)

    fun unregisterAll(owner: ModuleContainerView): Int

    fun unregisterAllById(ownerId: String): Int

    fun getCommand(name: String): ARCCommand?

    fun getCommands(): Collection<ARCCommand>

    fun registeredBy(owner: ModuleContainerView): Set<String>

    fun registeredById(ownerId: String): Set<String>
}
