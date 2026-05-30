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
        authors: List<String> = emptyList(),
        depends: List<String> = emptyList(),
        dependPlugins: List<String> = emptyList(),
        libraries: List<String> = emptyList(),
        apiVersion: String = "1.0"
    ): String = buildString {
        append("{")
        append("\"id\":\"$id\"")
        append(",\"name\":\"$name\"")
        append(",\"version\":\"$version\"")
        append(",\"main\":\"$mainClass\"")
        append(",\"description\":\"$description\"")
        if (authors.isNotEmpty()) append(",\"authors\":${jsonStringArray(authors)}")
        if (depends.isNotEmpty()) append(",\"depends\":${jsonStringArray(depends)}")
        if (dependPlugins.isNotEmpty()) append(",\"dependPlugins\":${jsonStringArray(dependPlugins)}")
        if (libraries.isNotEmpty()) append(",\"libraries\":${jsonStringArray(libraries)}")
        append(",\"apiVersion\":\"$apiVersion\"")
        append("}")
    }

    private fun jsonStringArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
}
