package cc.arccore.core.command

import cc.arccore.diagnostics.leak.LeakDetectionManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ArcLeaksCommand(
    private val leakDetectionManager: LeakDetectionManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("arccore.admin.leaks")) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }

        when (args.getOrNull(1)?.lowercase()) {
            null, "all" -> {
                val leaks = leakDetectionManager.getAllLeaks()
                leakDetectionManager.getReporter().formatSummary(leaks).forEach { sender.sendMessage(it) }
            }
            "integrity" -> {
                val reports = leakDetectionManager.validateIntegrity()
                leakDetectionManager.getReporter().formatSummary(reports).forEach { sender.sendMessage(it) }
            }
            "verify" -> {
                val moduleId = args.getOrNull(2) ?: run {
                    sender.sendMessage("§7Usage: /arc leaks verify <moduleId> [gc]")
                    return true
                }
                val gcHint = args.getOrNull(3)?.lowercase() == "gc"
                if (gcHint) sender.sendMessage("§7[ARCCore] Triggering GC hint before verification...")
                val result = leakDetectionManager.verifyUnload(moduleId, gcHint)
                leakDetectionManager.getReporter().formatVerificationResult(result).forEach { sender.sendMessage(it) }
            }
            "module" -> {
                val moduleId = args.getOrNull(2) ?: run {
                    sender.sendMessage("§7Usage: /arc leaks module <moduleId>")
                    return true
                }
                val leaks = leakDetectionManager.getLeaksForModule(moduleId)
                if (leaks.isEmpty()) {
                    sender.sendMessage("§7[ARCCore] §aNo leaks recorded for module '§e${moduleId}§a'.")
                } else {
                    leakDetectionManager.getReporter().formatSummary(leaks).forEach { sender.sendMessage(it) }
                }
            }
            "clear" -> {
                val moduleId = args.getOrNull(2) ?: run {
                    sender.sendMessage("§7Usage: /arc leaks clear <moduleId>")
                    return true
                }
                leakDetectionManager.clearModuleLeaks(moduleId)
                sender.sendMessage("§7[ARCCore] §aLeak records cleared for module '§e${moduleId}§a'.")
            }
            else -> {
                sender.sendMessage("§7Usage: /arc leaks [all|integrity|verify <id> [gc]|module <id>|clear <id>]")
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<out String>
    ): List<String> {
        return when {
            args.size == 2 -> listOf("all", "integrity", "verify", "module", "clear")
                .filter { it.startsWith(args[1].lowercase()) }
            args.size == 3 && args[1].lowercase() in setOf("verify", "module", "clear") -> {
                runCatching {
                    leakDetectionManager.getAllLeaks().map { it.moduleId }.distinct()
                        .filter { it.startsWith(args[2].lowercase()) }.sorted()
                }.getOrDefault(emptyList())
            }
            args.size == 4 && args[1].lowercase() == "verify" ->
                listOf("gc").filter { it.startsWith(args[3].lowercase()) }
            else -> emptyList()
        }
    }
}
