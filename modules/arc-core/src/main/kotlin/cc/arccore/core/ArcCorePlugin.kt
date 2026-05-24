package cc.arccore.core

import cc.arccore.api.ArcPlugin
import org.bukkit.plugin.java.JavaPlugin

class ArcCorePlugin : JavaPlugin() {

    lateinit var arcPlugin: ArcPlugin
        private set

    override fun onLoad() {
        instance = this
        arcPlugin = ArcCorePluginDelegate(this)
        arcPlugin.onLoad()
    }

    override fun onEnable() {
        arcPlugin.onEnable()
    }

    override fun onDisable() {
        arcPlugin.onDisable()
    }

    companion object {
        lateinit var instance: ArcCorePlugin
            private set
    }
}
