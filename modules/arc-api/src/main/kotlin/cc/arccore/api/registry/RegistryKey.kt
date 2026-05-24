package cc.arccore.api.registry

data class RegistryKey(
    val namespace: String,
    val name: String
) {
    override fun toString(): String = "$namespace:$name"

    companion object {
        fun of(namespace: String, name: String): RegistryKey = RegistryKey(namespace, name)
        fun parse(value: String): RegistryKey {
            val parts = value.split(":", limit = 2)
            require(parts.size == 2) { "Invalid RegistryKey format: $value" }
            return RegistryKey(parts[0], parts[1])
        }
    }
}
