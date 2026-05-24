package cc.arccore.api.command

interface CommandSender {
	val name: String
	fun hasPermission(permission: String): Boolean
	fun sendMessage(message: String)
}