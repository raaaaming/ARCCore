package cc.arccore.command

import cc.arccore.api.command.ARCCommand
import cc.arccore.api.command.CommandRegistry
import cc.arccore.api.command.exception.DuplicateCommandException
import cc.arccore.api.module.ModuleContainerView
import java.util.concurrent.ConcurrentHashMap

class DefaultCommandRegistry : CommandRegistry {

    private data class CommandEntry(
        val command: ARCCommand,
        val primaryName: String,
        val ownerId: String
    )

    private val entries = ConcurrentHashMap<String, CommandEntry>()

    override fun register(command: ARCCommand, owner: ModuleContainerView, override: Boolean) {
        val ownerId = owner.module.id
        val primaryName = command.metadata.name

        if (override) {
            // 기존 항목이 있으면 해당 primaryName의 모든 alias 포함 제거 후 재등록
            removeByPrimaryName(primaryName)
        }

        for (name in command.metadata.allNames) {
            val key = name.lowercase()
            val existing = entries.putIfAbsent(key, CommandEntry(command, primaryName, ownerId))
            if (existing != null) {
                // 이미 등록된 alias/name 충돌 — 이미 넣은 것도 롤백
                command.metadata.allNames.takeWhile { it != name }.forEach { prev ->
                    entries.remove(prev.lowercase())
                }
                throw DuplicateCommandException(key, existing.ownerId)
            }
        }
    }

    override fun unregister(name: String) {
        val key = name.lowercase()
        val entry = entries[key] ?: return
        val primaryName = entry.primaryName
        // primaryName과 일치하는 모든 키를 제거
        removeByPrimaryName(primaryName)
    }

    override fun unregisterAll(owner: ModuleContainerView): Int =
        unregisterAllById(owner.module.id)

    override fun unregisterAllById(ownerId: String): Int {
        var primaryCount = 0
        val seenPrimaries = mutableSetOf<String>()
        val iter = entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.value.ownerId == ownerId) {
                if (seenPrimaries.add(entry.value.primaryName)) {
                    primaryCount++
                }
                iter.remove()
            }
        }
        return primaryCount
    }

    override fun getCommand(name: String): ARCCommand? =
        entries[name.lowercase()]?.command

    override fun getCommands(): Collection<ARCCommand> =
        entries.values.distinctBy { it.primaryName }.map { it.command }

    override fun registeredBy(owner: ModuleContainerView): Set<String> =
        registeredById(owner.module.id)

    override fun registeredById(ownerId: String): Set<String> =
        entries.values
            .filter { it.ownerId == ownerId }
            .map { it.primaryName }
            .toSet()

    // 동일 primaryName을 가진 모든 엔트리를 제거한다.
    private fun removeByPrimaryName(primaryName: String) {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            if (iter.next().value.primaryName == primaryName) {
                iter.remove()
            }
        }
    }
}
