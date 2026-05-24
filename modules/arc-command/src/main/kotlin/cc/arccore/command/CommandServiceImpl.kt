package cc.arccore.command

import cc.arccore.api.command.Command
import cc.arccore.api.command.CommandManager

class CommandServiceImpl : CommandManager {

    private val commands = mutableMapOf<String, Command>()

    override fun register(command: Command) {
        commands[command.name.lowercase()] = command
        command.aliases.forEach { alias ->
            commands[alias.lowercase()] = command
        }
    }

    override fun unregister(name: String) {
        commands.remove(name.lowercase())
    }

    override fun getCommand(name: String): Command? = commands[name.lowercase()]

    override fun getCommands(): Collection<Command> = commands.values.distinct()
}
