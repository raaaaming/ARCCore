package cc.arccore.event

import cc.arccore.api.event.Event
import cc.arccore.api.event.EventHandler
import cc.arccore.api.event.EventManager

class EventServiceImpl : EventManager {

    private val handlers = mutableMapOf<Class<out Event>, MutableList<EventHandler>>()

    override fun register(handler: EventHandler, vararg eventTypes: Class<out Event>) {
        eventTypes.forEach { type ->
            handlers.getOrPut(type) { mutableListOf() }.add(handler)
        }
    }

    override fun unregister(handler: EventHandler) {
        handlers.values.forEach { it.remove(handler) }
    }

    override fun post(event: Event) {
        handlers[event::class.java]?.forEach { handler ->
            handler.handle(event)
        }
    }
}
