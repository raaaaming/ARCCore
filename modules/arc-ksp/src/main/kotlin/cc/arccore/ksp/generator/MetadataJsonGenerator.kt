package cc.arccore.ksp.generator

import cc.arccore.ksp.model.CommandEntry
import cc.arccore.ksp.model.ListenerEntry
import cc.arccore.ksp.model.ServiceEntry

object MetadataJsonGenerator {

    fun generateCommands(entries: List<CommandEntry>): String {
        val entriesJson = entries.joinToString(",") { entry ->
            buildString {
                append("{\"className\":\"${entry.className}\"")
                if (entry.commandName.isNotBlank()) {
                    append(",\"name\":\"${entry.commandName}\"")
                    append(",\"description\":\"${entry.commandDescription}\"")
                    append(",\"usage\":\"${entry.commandUsage}\"")
                    append(",\"permission\":\"${entry.commandPermission}\"")
                    if (entry.commandAliases.isNotEmpty()) {
                        val aliasJson = entry.commandAliases.joinToString(",") { "\"$it\"" }
                        append(",\"aliases\":[$aliasJson]")
                    }
                }
                if (entry.constructorParams.isNotEmpty()) {
                    val paramsJson = entry.constructorParams.joinToString(",") {
                        "{\"type\":\"${it.typeFqn}\",\"nullable\":${it.nullable}}"
                    }
                    append(",\"constructorParams\":[$paramsJson]")
                }
                append("}")
            }
        }
        return "{\"version\":1,\"entries\":[$entriesJson]}"
    }

    fun generateListeners(entries: List<ListenerEntry>): String {
        val entriesJson = entries.joinToString(",") { "{\"className\":\"${it.className}\"}" }
        return "{\"version\":1,\"entries\":[$entriesJson]}"
    }

    fun generateServices(entries: List<ServiceEntry>): String {
        val entriesJson = entries.joinToString(",") {
            "{\"className\":\"${it.className}\",\"name\":\"${it.name}\"}"
        }
        return "{\"version\":1,\"entries\":[$entriesJson]}"
    }

    fun generateModuleManifest(
        id: String,
        name: String,
        version: String,
        mainClass: String,
        description: String,
        apiVersion: String = "1.0"
    ): String = """{"id":"$id","name":"$name","version":"$version","main":"$mainClass","description":"$description","apiVersion":"$apiVersion"}"""
}
