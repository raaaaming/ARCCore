package cc.arccore.api.command

interface CommandManager {
    fun register(command: Command)
    fun unregister(name: String)
    fun getCommand(name: String): Command?
    fun getCommands(): Collection<Command>
}
