package cc.arccore.di.container

import cc.arccore.api.di.exception.CircularDependencyException
import kotlin.reflect.KClass

class CircularDependencyValidator {

    private val resolutionChain = ThreadLocal.withInitial<LinkedHashSet<String>> { LinkedHashSet() }

    fun enter(type: KClass<*>) {
        val fqn = type.qualifiedName ?: type.simpleName ?: type.toString()
        val chain = resolutionChain.get()
        if (!chain.add(fqn)) {
            throw CircularDependencyException(chain.toList() + fqn)
        }
    }

    fun exit(type: KClass<*>) {
        val fqn = type.qualifiedName ?: type.simpleName ?: type.toString()
        val chain = resolutionChain.get()
        chain.remove(fqn)
        if (chain.isEmpty()) {
            resolutionChain.remove()
        }
    }
}
