package cc.arccore.runtime.context.access

import cc.arccore.api.command.ARCCommand
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.Plugin

class BukkitCommandBridge(private val plugin: Plugin) {

    private val commandMap: SimpleCommandMap by lazy {
        val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        field.get(Bukkit.getServer()) as SimpleCommandMap
    }

    @Suppress("UNCHECKED_CAST")
    private val knownCommands: MutableMap<String, Command> by lazy {
        val field = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
        field.isAccessible = true
        field.get(commandMap) as MutableMap<String, Command>
    }

    fun register(command: ARCCommand) {
        val adapter = BukkitCommandAdapter(command)
        commandMap.register(plugin.name.lowercase(), adapter)
    }

    fun unregister(name: String) {
        val key = name.lowercase()
        val cmd = knownCommands[key] ?: knownCommands["${plugin.name.lowercase()}:$key"] ?: return
        knownCommands.values.removeAll { it === cmd }
        cmd.unregister(commandMap)
    }
}
