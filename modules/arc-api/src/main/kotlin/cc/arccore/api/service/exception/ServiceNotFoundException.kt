package cc.arccore.api.service.exception

import kotlin.reflect.KClass

class ServiceNotFoundException(val serviceType: KClass<*>)
    : RuntimeException("Service not found: ${serviceType.qualifiedName}")
