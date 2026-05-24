package cc.arccore.api.event

interface EventManager {
    fun register(handler: EventHandler, vararg eventTypes: Class<out Event>)
    fun unregister(handler: EventHandler)
    fun post(event: Event)
}
