package cc.arccore.diagnostics.leak.lifecycle

import cc.arccore.api.lifecycle.FilteredLifecycleObserver
import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleEventType
import cc.arccore.api.module.ClassLoaderHolder
import cc.arccore.diagnostics.leak.weakref.ClassLoaderLeakTracker

/**
 * Hooks into the module lifecycle to capture ClassLoaders at LOADED time
 * and schedule post-unload verification at UNLOADED time.
 *
 * Why capture at LOADED (not UNLOADED):
 *   By the time UNLOADED fires, ModuleContainer.transitionToUnloaded() has already
 *   set context = null. The ClassLoaderHolder interface is only accessible while
 *   the context is live. We must capture the WeakReference before the context is nulled.
 *
 * Capturing at LOADED ensures we always have a WeakReference for the current generation.
 * On hot-reload the tracker overwrites the previous entry — this is intentional.
 */
class LeakDetectionLifecycleObserver(
    private val classLoaderTracker: ClassLoaderLeakTracker,
    private val onUnloadedCallback: (moduleId: String) -> Unit = {}
) : FilteredLifecycleObserver(
    LifecycleEventType.LOADED,
    LifecycleEventType.UNLOADED
) {

    override fun onAcceptedEvent(event: LifecycleEvent) {
        val moduleId = event.container.module.id

        when (event.type) {
            LifecycleEventType.LOADED -> captureClassLoader(moduleId, event)
            LifecycleEventType.UNLOADED -> {
                // Context is null by now; we only notify for post-unload verification.
                onUnloadedCallback(moduleId)
            }
            else -> Unit
        }
    }

    private fun captureClassLoader(moduleId: String, event: LifecycleEvent) {
        val context = event.container.context ?: return
        val classLoader = (context as? ClassLoaderHolder)?.provideClassLoader() ?: return
        classLoaderTracker.track(moduleId, classLoader)
    }
}
