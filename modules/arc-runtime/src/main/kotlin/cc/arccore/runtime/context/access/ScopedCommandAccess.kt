package cc.arccore.runtime.context.access

import cc.arccore.api.command.ARCCommand

interface ScopedCommandAccess {
    fun register(command: ARCCommand, override: Boolean = false, autoCleanup: Boolean = true)
    fun unregister(name: String)
    fun unregisterAll(): Int
    fun getCommand(name: String): ARCCommand?
    fun registeredNames(): Set<String>
}
