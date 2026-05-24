package cc.arccore.api.command

interface ARCCommand {
    val metadata: CommandMetadata
        get() = throw UnsupportedOperationException(
            "${this::class.simpleName} has no metadata. Add @CommandSpec annotation or override metadata manually."
        )
    fun execute(context: CommandContext): CommandResult
    fun onTabComplete(context: CommandContext): List<String> = emptyList()
}
