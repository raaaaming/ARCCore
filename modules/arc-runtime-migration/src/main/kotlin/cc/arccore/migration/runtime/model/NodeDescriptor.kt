package cc.arccore.migration.runtime.model

data class NodeDescriptor(
    val nodeId: String,
    val nodeType: NodeType,
    val capacity: NodeCapacity = NodeCapacity.UNLIMITED,
    val metadata: Map<String, String> = emptyMap()
)

enum class NodeType {
    LOCAL,
    REMOTE_PLACEHOLDER
}

data class NodeCapacity(val maxModules: Int, val availableModules: Int) {
    companion object {
        val UNLIMITED = NodeCapacity(Int.MAX_VALUE, Int.MAX_VALUE)
    }
}
