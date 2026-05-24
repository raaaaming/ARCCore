package cc.arccore.runtime.unload.cleanup

enum class CleanupPriority(
    val order: Int
) {
    /** 비활성화 전 커스텀 사전 처리 */
    PRE_DISABLE(50),
    /** Paper/Bukkit 스케줄러 Task 취소 */
    SCHEDULER(100),
    /** Bukkit 이벤트 리스너 해제 */
    LISTENER(200),
    /** 모듈의 CleanupAware.onCleanupService() 훅 호출 — 모듈이 직접 관리하는 서비스 자원 해제 */
    SERVICE(300),
    /** 글로벌 ServiceRegistry에서 이 모듈이 등록한 서비스를 일괄 해제 */
    SERVICE_REGISTRY(310),
    /** 모듈의 Storage 핸들(config/file/cache/database)을 일괄 닫음 */
    STORAGE(320),
    /** 커맨드 등록 해제 */
    COMMAND(400),
    /** 모듈 레지스트리 등록 해제 */
    REGISTRY(500),
    /** 언로드 전 처리 — CleanupScope.close() 포함 */
    PRE_UNLOAD(550),
    /** URLClassLoader.close() 호출 */
    CLASSLOADER(600),
    /** 언로드 후 정리 (레거시: REFERENCE와 동일) */
    POST_UNLOAD(650),
    /** ModuleClassLoaderTracker 등록 — GC 누수 감시 시작 */
    CLASSLOADER_RELEASE(700);

    companion object {
        fun sortedAscending(): List<CleanupPriority> =
            entries.sortedBy { it.order }
    }
}
