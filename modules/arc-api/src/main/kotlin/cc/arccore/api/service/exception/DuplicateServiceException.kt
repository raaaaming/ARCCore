package cc.arccore.api.service.exception

import kotlin.reflect.KClass

class DuplicateServiceException(val serviceType: KClass<*>, val existingOwner: String)
    : RuntimeException("Service already registered: ${serviceType.qualifiedName} (owner: $existingOwner). Use override=true to replace.")
