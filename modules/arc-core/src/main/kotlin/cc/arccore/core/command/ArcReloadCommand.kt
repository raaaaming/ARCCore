package cc.arccore.core.command

import cc.arccore.api.module.reload.ReloadResult
import cc.arccore.runtime.reload.HotReloadManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ArcReloadCommand(
    private val hotReloadManager: HotReloadManager,
    private val moduleIdProvider: () -> Set<String>
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("arccore.admin.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }
        if (args.isEmpty() || args[0].lowercase() != "reload") {
            sender.sendMessage("§7Usage: /arc reload <moduleId>")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§7Usage: /arc reload <moduleId>")
            return true
        }

        val moduleId = args[1]

        if (hotReloadManager.isReloading()) {
            sender.sendMessage("§eA reload is already in progress.")
            return true
        }

        sender.sendMessage("§7[ARCCore] §fReloading module '§e$moduleId§f'...")

        val result = hotReloadManager.reload(moduleId)
        sender.sendMessage(formatResult(result))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when {
            args.size == 1 -> listOf("reload").filter { it.startsWith(args[0].lowercase()) }
            args.size == 2 && args[0].lowercase() == "reload" ->
                moduleIdProvider().filter { it.startsWith(args[1].lowercase()) }.sorted()
            else -> emptyList()
        }
    }

    private fun formatResult(result: ReloadResult): String = when (result) {
        is ReloadResult.Success ->
            "§7[ARCCore] §a✓ §f'§e${result.moduleId}§f' reload complete" +
                (if (result.affectedModules.isNotEmpty())
                    " (affected: ${result.affectedModules.joinToString(", ")})"
                else "") +
                " §7(${result.elapsedMs}ms)"
        is ReloadResult.Failure ->
            "§7[ARCCore] §c✗ §f'§e${result.moduleId}§f' reload FAILED at §c${result.phase}§f: ${result.error.message}"
        is ReloadResult.PartialSuccess ->
            "§7[ARCCore] §e⚠ §f'§e${result.moduleId}§f' reload partial — " +
                "failed: ${result.failedModules.joinToString(", ") { it.moduleId }}"
        is ReloadResult.Rejected ->
            "§7[ARCCore] §e⊘ §f'§e${result.moduleId}§f' reload rejected: ${result.reason}"
        is ReloadResult.AlreadyReloading ->
            "§7[ARCCore] §eA reload is already in progress."
    }
}
