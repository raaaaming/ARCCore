package cc.arccore.runtime.lifecycle

import cc.arccore.api.lifecycle.LifecycleEvent
import cc.arccore.api.lifecycle.LifecycleObserver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

/**
 * 생명주기 이벤트를 등록된 LifecycleObserver들에게 발행합니다.
 * CopyOnWriteArrayList를 사용하여 이벤트 디스패치 중 리스너 추가/제거를 허용합니다.
 * 옵저버 예외는 격리되어 다른 옵저버 실행에 영향을 미치지 않습니다.
 */
class LifecycleEventBus {

    private val observers = CopyOnWriteArrayList<LifecycleObserver>()
    private val log = Logger.getLogger(LifecycleEventBus::class.java.name)

    fun addObserver(observer: LifecycleObserver) {
        observers.addIfAbsent(observer)
    }

    fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    fun publish(event: LifecycleEvent) {
        for (observer in observers) {
            try {
                observer.onLifecycleEvent(event)
            } catch (e: Exception) {
                log.warning(
                    "LifecycleObserver ${observer::class.java.simpleName} threw exception " +
                        "on event ${event.type} for module '${event.container.module.id}': ${e.message}"
                )
            }
        }
    }

    fun observerCount(): Int = observers.size
}
