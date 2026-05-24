package cc.arccore.api.command

data class CommandContext(
    val sender: CommandSender,
    val label: String,
    val args: List<String>,
    val rawArgs: Array<String> = emptyArray()
) {
    val hasArgs: Boolean get() = args.isNotEmpty()
    val firstArg: String? get() = args.firstOrNull()
    val subCommand: String? get() = firstArg?.lowercase()
    val remainingArgs: List<String> get() = if (args.size > 1) args.drop(1) else emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandContext) return false
        return sender == other.sender &&
            label == other.label &&
            args == other.args &&
            rawArgs.contentEquals(other.rawArgs)
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + args.hashCode()
        result = 31 * result + rawArgs.contentHashCode()
        return result
    }
}
