package cc.arccore.event.exception

class DuplicateListenerException(
    listenerClass: String,
    ownerId: String
) : ListenerRegistrationException(
    "Listener '$listenerClass' is already registered for module '$ownerId'"
)
