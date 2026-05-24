package cc.arccore.runtime.resource.validation

import cc.arccore.runtime.resource.ResourceDescriptor
import cc.arccore.runtime.resource.exception.InvalidResourceStateException
import cc.arccore.runtime.resource.exception.ResourceOwnershipException

object ResourceValidator {

    fun requireNotReleased(descriptor: ResourceDescriptor) {
        if (descriptor.isReleased) throw InvalidResourceStateException(
            resourceId = descriptor.id.toString(),
            currentState = descriptor.state.name,
            attemptedOperation = "access already-released resource '${descriptor.name}'"
        )
    }

    fun requireValidOwner(moduleId: String) {
        if (moduleId.isBlank()) throw ResourceOwnershipException(
            resourceId = "unknown",
            message = "moduleId must not be blank for resource ownership"
        )
    }

    fun requireValidName(name: String) {
        if (name.isBlank()) throw ResourceOwnershipException(
            resourceId = "unknown",
            message = "resource name must not be blank"
        )
    }
}
