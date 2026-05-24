package cc.arccore.runtime.annotation.scanner

import cc.arccore.runtime.annotation.exception.AnnotationScanException
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KClass

class ReflectionScanner : AnnotationScanner {

    override fun scan(
        classLoader: ClassLoader,
        moduleId: String,
        targetAnnotations: Set<KClass<out Annotation>>
    ): List<ScanResult> {
        if (targetAnnotations.isEmpty()) return emptyList()

        val urls = when (classLoader) {
            is URLClassLoader -> classLoader.urLs.toList()
            else -> return emptyList()
        }

        val results = mutableListOf<ScanResult>()
        val annotationClassMap: Map<Class<out Annotation>, KClass<out Annotation>> =
            targetAnnotations.associateBy { it.java }

        for (url in urls) {
            val file = try {
                File(url.toURI())
            } catch (_: Exception) {
                continue
            }
            if (!file.exists() || !file.name.endsWith(".jar")) continue

            val jarFile = try {
                JarFile(file)
            } catch (e: Exception) {
                throw AnnotationScanException("Cannot open JAR for module '$moduleId': ${file.path}", e)
            }

            jarFile.use { jar ->
                jar.entries().asSequence()
                    .filter { entry ->
                        entry.name.endsWith(".class")
                            && !entry.name.contains('$')
                            && !entry.isDirectory
                    }
                    .forEach { entry ->
                        val className = entry.name
                            .removeSuffix(".class")
                            .replace('/', '.')
                        try {
                            val clazz = classLoader.loadClass(className)
                            for ((annoClass, annoKClass) in annotationClassMap) {
                                if (clazz.isAnnotationPresent(annoClass)) {
                                    results.add(ScanResult(clazz, annoKClass, moduleId))
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
            }
        }

        return results
    }
}
