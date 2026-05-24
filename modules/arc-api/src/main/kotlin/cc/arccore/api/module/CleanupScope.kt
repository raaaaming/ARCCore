package cc.arccore.api.module

/**
 * 모듈 언로드 시 자동으로 해제되는 리소스 범위.
 *
 * 모듈은 [onLoad] 내에서 [ModuleContext.cleanupScope]를 통해 접근하며,
 * 리스너, 태스크, 커스텀 클린업 액션을 등록한다.
 * 언로드 파이프라인이 [close]를 호출하면 등록된 리소스가 역순(LIFO)으로 해제된다.
 *
 * arc-api 레벨에서는 Bukkit/Paper 타입에 의존하지 않는다.
 * Bukkit 리스너/태스크 등록 헬퍼는 arc-runtime의 확장 함수로 제공된다.
 */
interface CleanupScope : AutoCloseable {

    /** 이미 close()된 경우 true */
    val isClosed: Boolean

    /**
     * [AutoCloseable] 리소스를 등록한다.
     * close() 호출 시 역순으로 [AutoCloseable.close]가 호출된다.
     */
    fun register(closeable: AutoCloseable)

    /**
     * 클린업 람다를 등록한다.
     * close() 호출 시 역순으로 실행된다.
     */
    fun onClose(action: () -> Unit)

    /**
     * 이름 기반으로 리소스를 등록한다. 동일한 key로 재등록 시 이전 리소스를 즉시 해제하고 교체한다.
     */
    fun register(key: String, closeable: AutoCloseable)

    /**
     * 특정 key로 등록된 리소스를 즉시 해제하고 scope에서 제거한다.
     */
    fun release(key: String)

    /**
     * 등록된 모든 리소스를 역순으로 해제한다.
     * 이미 close()된 경우 아무것도 하지 않는다 (idempotent).
     */
    override fun close()
}
