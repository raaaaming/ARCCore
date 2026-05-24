package cc.arccore.loader.classloader

import cc.arccore.loader.ModuleClassLoader
import java.net.URL
import java.util.*

class ModuleResourceLoader(
    private val classLoader: ModuleClassLoader,
    private val parent: ClassLoader,
    private val sharedFilter: SharedClassFilter,
    private val strategy: ClassLoaderStrategy,
    private val dependencyClassLoaders: List<ModuleClassLoader>
) {
    fun getResource(name: String): URL? {
        return when (strategy) {
            ClassLoaderStrategy.PARENT_FIRST -> getResourceParentFirst(name)
            ClassLoaderStrategy.CHILD_FIRST -> getResourceChildFirst(name)
            ClassLoaderStrategy.HYBRID -> getResourceHybrid(name)
        }
    }

    fun getResources(name: String): Enumeration<URL> {
        return when (strategy) {
            ClassLoaderStrategy.PARENT_FIRST -> getResourcesParentFirst(name)
            ClassLoaderStrategy.CHILD_FIRST -> getResourcesChildFirst(name)
            ClassLoaderStrategy.HYBRID -> getResourcesHybrid(name)
        }
    }

    private fun getResourceParentFirst(name: String): URL? {
        val parentResource = parent.getResource(name)
        if (parentResource != null) return parentResource
        for (dep in dependencyClassLoaders) {
            val depResource = dep.getResource(name)
            if (depResource != null) return depResource
        }
        return classLoader.findResource(name)
    }

    private fun getResourceChildFirst(name: String): URL? {
        val selfResource = classLoader.findResource(name)
        if (selfResource != null) return selfResource
        for (dep in dependencyClassLoaders) {
            val depResource = dep.getResource(name)
            if (depResource != null) return depResource
        }
        return parent.getResource(name)
    }

    private fun getResourceHybrid(name: String): URL? {
        return if (isSharedResource(name)) {
            getResourceParentFirst(name)
        } else {
            getResourceChildFirst(name)
        }
    }

    private fun getResourcesParentFirst(name: String): Enumeration<URL> {
        val result = LinkedHashSet<URL>()
        val parentResources = parent.getResources(name)
        while (parentResources.hasMoreElements()) {
            result.add(parentResources.nextElement())
        }
        for (dep in dependencyClassLoaders) {
            val depResources = dep.getResources(name)
            while (depResources.hasMoreElements()) {
                result.add(depResources.nextElement())
            }
        }
        val selfResources = classLoader.findResources(name)
        while (selfResources.hasMoreElements()) {
            result.add(selfResources.nextElement())
        }
        return Collections.enumeration(result.toList())
    }

    private fun getResourcesChildFirst(name: String): Enumeration<URL> {
        val result = LinkedHashSet<URL>()
        val selfResources = classLoader.findResources(name)
        while (selfResources.hasMoreElements()) {
            result.add(selfResources.nextElement())
        }
        for (dep in dependencyClassLoaders) {
            val depResources = dep.getResources(name)
            while (depResources.hasMoreElements()) {
                result.add(depResources.nextElement())
            }
        }
        val parentResources = parent.getResources(name)
        while (parentResources.hasMoreElements()) {
            result.add(parentResources.nextElement())
        }
        return Collections.enumeration(result.toList())
    }

    private fun getResourcesHybrid(name: String): Enumeration<URL> {
        return if (isSharedResource(name)) {
            getResourcesParentFirst(name)
        } else {
            getResourcesChildFirst(name)
        }
    }

    private fun isSharedResource(resourceName: String): Boolean {
        val className = if (resourceName.endsWith(".class")) {
            resourceName.removeSuffix(".class").replace('/', '.')
        } else {
            null
        }
        if (className != null && sharedFilter.isShared(className)) {
            return true
        }
        return false
    }
}
