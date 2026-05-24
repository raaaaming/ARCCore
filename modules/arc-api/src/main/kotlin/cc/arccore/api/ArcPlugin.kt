package cc.arccore.api

interface ArcPlugin {
    val pluginId: String
    val pluginVersion: String
    val api: ArcAPI

    fun onLoad()
    fun onEnable()
    fun onDisable()
}
