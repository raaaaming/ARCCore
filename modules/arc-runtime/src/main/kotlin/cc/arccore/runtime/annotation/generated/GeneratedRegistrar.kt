package cc.arccore.runtime.annotation.generated

import cc.arccore.runtime.context.RuntimeModuleContext
import org.bukkit.plugin.Plugin

interface GeneratedRegistrar {
    fun register(context: RuntimeModuleContext, plugin: Plugin)
}
