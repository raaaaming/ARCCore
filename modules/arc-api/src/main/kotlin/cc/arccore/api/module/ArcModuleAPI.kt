package cc.arccore.api.module

import cc.arccore.api.exception.ModuleStateException

interface ArcModuleAPI {

    val id: String

    val description: ModuleDescription

    fun onLoad(context: ModuleContext)

    fun onEnable()

    fun onDisable()

    fun onUnload()
}
