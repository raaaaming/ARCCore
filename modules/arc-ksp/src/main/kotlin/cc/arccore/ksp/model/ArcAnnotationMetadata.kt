package cc.arccore.ksp.model

data class ConstructorParam(val typeFqn: String, val nullable: Boolean)

data class CommandEntry(
    val className: String,
    val constructorParams: List<ConstructorParam> = emptyList(),
    val commandName: String = "",
    val commandAliases: List<String> = emptyList(),
    val commandPermission: String = "",
    val commandDescription: String = "",
    val commandUsage: String = ""
)

data class ListenerEntry(
    val className: String,
    val constructorParams: List<ConstructorParam> = emptyList()
)

data class ServiceEntry(
    val className: String,
    val name: String
)
