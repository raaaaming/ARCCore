package cc.arccore.core.command

import cc.arccore.diagnostics.RuntimeDiagnosticsManager
import cc.arccore.runtime.resource.ResourceTracker
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ArcDiagnosticsCommand(
    private val diagnosticsManager: RuntimeDiagnosticsManager,
    private val resourceTracker: ResourceTracker? = null
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("arccore.admin.diagnostics")) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }

        val subcommand = args.getOrNull(1)?.lowercase()

        when (subcommand) {
            null, "all" -> {
                val snapshot = diagnosticsManager.takeSnapshot()
                diagnosticsManager.getReporter().formatFull(snapshot).forEach { sender.sendMessage(it) }
            }
            "module" -> {
                val moduleId = args.getOrNull(2) ?: run {
                    sender.sendMessage("§7Usage: /arc diagnostics module <moduleId>")
                    return true
                }
                val snapshot = diagnosticsManager.takeSnapshot()
                val moduleSnap = snapshot.modules.find { it.moduleId == moduleId }
                if (moduleSnap == null) {
                    sender.sendMessage("§c[ARCCore] Module '§e${moduleId}§c' not found.")
                } else {
                    diagnosticsManager.getReporter().formatModuleDetail(moduleSnap).forEach { sender.sendMessage(it) }
                }
            }
            "metrics" -> {
                val metrics = diagnosticsManager.collectMetrics()
                diagnosticsManager.getReporter().formatMetrics(metrics).forEach { sender.sendMessage(it) }
            }
            "resources" -> {
                val tracker = resourceTracker
                if (tracker == null) {
                    sender.sendMessage("§c[ARCCore] Resource tracker not available.")
                    return true
                }
                val subArg = args.getOrNull(2)?.lowercase()
                if (subArg != null && subArg != "all") {
                    val moduleSnapshot = tracker.getModuleSnapshot(subArg)
                    tracker.getReporter().formatModuleDetail(moduleSnapshot).forEach { sender.sendMessage(it) }
                } else {
                    val snapshot = tracker.takeSnapshot()
                    tracker.getReporter().formatFull(snapshot).forEach { sender.sendMessage(it) }
                }
            }
            else -> {
                sender.sendMessage("§7Usage: /arc diagnostics [all|module <id>|metrics|resources [moduleId]]")
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<out String>
    ): List<String> {
        return when {
            args.size == 2 -> listOf("all", "module", "metrics", "resources")
                .filter { it.startsWith(args[1].lowercase()) }
            args.size == 3 && args[1].lowercase() == "module" -> {
                runCatching {
                    diagnosticsManager.takeSnapshot().modules.map { it.moduleId }
                        .filter { it.startsWith(args[2].lowercase()) }.sorted()
                }.getOrDefault(emptyList())
            }
            else -> emptyList()
        }
    }
}
