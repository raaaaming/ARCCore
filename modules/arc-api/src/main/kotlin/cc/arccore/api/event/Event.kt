package cc.arccore.api.event

interface Event

interface CancellableEvent : Event {
    var cancelled: Boolean
}

interface EventHandler {
    fun handle(event: Event)
}
