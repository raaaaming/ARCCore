package cc.arccore.api.command

data class CommandMetadata(
    val name: String,
    val aliases: List<String> = emptyList(),
    val permission: String? = null,
    val description: String = "",
    val usage: String = "/<command>"
) {
    init {
        require(name.isNotBlank()) { "Command name must not be blank" }
        require(name == name.lowercase()) { "Command name must be lowercase: '$name'" }
    }

    val allNames: List<String> get() = listOf(name) + aliases
}
