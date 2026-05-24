package cc.arccore.diagnostics.leak.weakref

import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks module ClassLoaders via WeakReference + ReferenceQueue.
 *
 * Why WeakReference:
 *   A strong reference to a ClassLoader pins every class loaded by it in the JVM metaspace.
 *   After module unload, no code should hold a strong reference to the old ClassLoader.
 *   WeakReference allows the GC to collect it; if the reference is NOT cleared after GC,
 *   something upstream still holds a strong reference — a classloader leak.
 *
 * Why ReferenceQueue:
 *   When the GC clears a WeakReference, it enqueues it in the associated ReferenceQueue.
 *   Polling the queue lets us detect GC collection without calling System.gc() and
 *   without continuous polling of WeakReference.get().
 */
class ClassLoaderLeakTracker {

    private val refQueue = ReferenceQueue<ClassLoader>()
    private val tracked = ConcurrentHashMap<String, WeakReference<ClassLoader>>()
    private val refToModuleId = ConcurrentHashMap<WeakReference<ClassLoader>, String>()

    fun track(moduleId: String, classLoader: ClassLoader) {
        val existing = tracked[moduleId]
        if (existing != null) refToModuleId.remove(existing)

        val ref = WeakReference(classLoader, refQueue)
        tracked[moduleId] = ref
        refToModuleId[ref] = moduleId
    }

    /**
     * Drains the ReferenceQueue and returns all module IDs whose ClassLoaders have been
     * collected by the GC since the last call. Call after System.gc() hint for best results.
     */
    fun drainCollected(): Set<String> {
        val collected = mutableSetOf<String>()
        var ref: Reference<out ClassLoader>?
        while (refQueue.poll().also { ref = it } != null) {
            val id = refToModuleId.remove(ref) ?: continue
            tracked.remove(id)
            collected += id
        }
        return collected
    }

    /**
     * Returns true if the ClassLoader for [moduleId] has been collected by GC
     * (i.e., the WeakReference was cleared), or if the module was never tracked.
     * Returns false if the classloader is still reachable — potential leak.
     */
    fun isGcCandidate(moduleId: String): Boolean {
        val ref = tracked[moduleId] ?: return true
        return ref.get() == null
    }

    fun remove(moduleId: String) {
        val ref = tracked.remove(moduleId)
        if (ref != null) refToModuleId.remove(ref)
    }

    fun trackedModules(): Set<String> = tracked.keys.toSet()

    fun leakingModules(): Set<String> = tracked.keys.filter { !isGcCandidate(it) }.toSet()
}
