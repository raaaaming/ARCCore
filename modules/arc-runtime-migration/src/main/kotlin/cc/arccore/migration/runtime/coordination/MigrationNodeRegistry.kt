package cc.arccore.migration.runtime.coordination

import cc.arccore.migration.runtime.model.NodeDescriptor
import java.util.concurrent.ConcurrentHashMap

internal class MigrationNodeRegistry {
    private val nodes = ConcurrentHashMap<String, NodeDescriptor>()

    fun register(node: NodeDescriptor) {
        nodes[node.nodeId] = node
    }

    fun unregister(nodeId: String): NodeDescriptor? = nodes.remove(nodeId)

    fun get(nodeId: String): NodeDescriptor? = nodes[nodeId]

    fun all(): List<NodeDescriptor> = nodes.values.toList()

    fun exists(nodeId: String): Boolean = nodes.containsKey(nodeId)
}
