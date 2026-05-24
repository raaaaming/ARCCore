package cc.arccore.api.command

interface Command {
    val name: String
    val aliases: List<String>
    val permission: String?
    val description: String
    fun execute(sender: CommandSender, args: List<String>): Boolean
}
