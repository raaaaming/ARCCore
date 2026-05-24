package cc.arccore.runtime.unload.leak

import cc.arccore.api.module.ModuleContainer
import java.lang.ref.WeakReference

object LeakDetector {

    fun detectPotentialLeaks(container: ModuleContainer): List<LeakWarning> {
        val warnings = mutableListOf<LeakWarning>()
        val module = container.module
        val loader = module::class.java.classLoader

        if (loader != null && loader !== this::class.java.classLoader) {
            val moduleClassLoaderRef = WeakReference(loader)
            val threadLeaks = scanThreads(moduleClassLoaderRef)
            warnings.addAll(threadLeaks)
        }

        return warnings
    }

    internal fun scanThreads(classLoaderRef: WeakReference<ClassLoader>): List<LeakWarning> {
        val warnings = mutableListOf<LeakWarning>()
        var tg = Thread.currentThread().threadGroup
        while (tg.parent != null) tg = tg.parent!!

        val threads = arrayOfNulls<Thread>(tg.activeCount() * 2 + 16)
        val count = tg.enumerate(threads, true)

        for (i in 0 until count) {
            val thread = threads[i] ?: continue
            val tcl = thread.contextClassLoader
            if (tcl != null && tcl === classLoaderRef.get()) {
                warnings.add(
                    LeakWarning(
                        type = "THREAD_CONTEXT_CLASSLOADER",
                        message = "Thread '${thread.name}' still holds reference to module ClassLoader as TCCL",
                        source = "Thread:${thread.name}",
                        severity = LeakWarning.LeakSeverity.HIGH
                    )
                )
            }
        }

        return warnings
    }

    data class LeakWarning(
        val type: String,
        val message: String,
        val source: String,
        val severity: LeakSeverity
    ) {
        enum class LeakSeverity { LOW, MEDIUM, HIGH, CRITICAL }
    }
}
