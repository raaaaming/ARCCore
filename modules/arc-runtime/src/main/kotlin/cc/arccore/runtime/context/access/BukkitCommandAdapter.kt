package cc.arccore.runtime.context.access

import cc.arccore.api.command.ARCCommand
import cc.arccore.api.command.CommandContext
import cc.arccore.api.command.CommandResult
import cc.arccore.api.command.CommandSender as ArcSender
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

internal class BukkitCommandAdapter(
    val arcCommand: ARCCommand
) : Command(
    arcCommand.metadata.name,
    arcCommand.metadata.description,
    arcCommand.metadata.usage,
    arcCommand.metadata.aliases
) {
    init {
        permission = arcCommand.metadata.permission
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val ctx = CommandContext(sender.toArc(), commandLabel, args.toList(), arrayOf(*args))
        val result = arcCommand.execute(ctx)
        when (result) {
            is CommandResult.NoPermission -> sender.sendMessage(result.message)
            is CommandResult.InvalidUsage -> sender.sendMessage(result.usage)
            is CommandResult.Failure -> if (result.message.isNotBlank()) sender.sendMessage(result.message)
            else -> Unit
        }
        return result.toHandled()
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> =
        arcCommand.onTabComplete(CommandContext(sender.toArc(), alias, args.toList(), arrayOf(*args)))

    private fun CommandSender.toArc(): ArcSender = object : ArcSender {
        override val name: String get() = this@toArc.name
        override fun hasPermission(permission: String) = this@toArc.hasPermission(permission)
        override fun sendMessage(message: String) = this@toArc.sendMessage(message)
    }
}
