# ARCCore

Kotlin 기반 Minecraft Paper 서버용 모듈 플러그인 프레임워크.

---

## 목차

- [모듈 만들기](#모듈-만들기)
- [모듈 의존성 선언](#모듈-의존성-선언)
- [로거 & 데이터 폴더](#로거--데이터-폴더)
- [CleanupScope — 자동 리소스 정리](#cleanupscope--자동-리소스-정리)
- [서비스 등록 & 조회](#서비스-등록--조회)
- [커맨드 등록](#커맨드-등록)
- [리스너 등록](#리스너-등록)
- [다른 모듈의 서비스 사용하기](#다른-모듈의-서비스-사용하기)
- [모듈 상태 & 라이프사이클 관찰](#모듈-상태--라이프사이클-관찰)
- [Hot Reload 힌트](#hot-reload-힌트)
- [의존성 주입 (DI)](#의존성-주입-di)
- [레지스트리](#레지스트리)

---

## 모듈 만들기

### `arc-module.json` 작성 (`META-INF/arc-module.json`) (비권장)

```json
{
  "id": "my-module",
  "name": "My Module",
  "version": "1.0.0",
  "main": "com.example.MyModule",
  "description": "내 첫 번째 ARCCore 모듈",
  "authors": ["YourName"],
  "depends": ["other-module"],
  "softDepends": ["optional-module"],
  "apiVersion": "1.0"
}
```

`depends`는 로드 실패 시 이 모듈도 실패 처리되는 필수 의존성,  
`softDepends`는 없어도 로드는 되는 선택 의존성입니다.

`arc-module.json` 파일은 `@ModuleSpec` 어노테이션에 의해 자동 생성됩니다.

### 모듈 클래스

```kotlin
@ModuleSpec(
    id = "my-module",
    name = "My Module",
    version = "1.0.0",
    authors = ["YourName"]
)
class MyModule : BaseModule() {

    override fun onEnable() {
        logger.info("활성화됨!")
    }

    override fun onDisable() {
        logger.info("비활성화됨.")
    }
}
```

`BaseModule`을 상속하면 `context`, `logger`, `dataFolder`를 바로 사용할 수 있습니다.  
이 세 가지는 `onLoad()` 이후부터 접근 가능합니다.

### `onLoad`에서 초기화하기

```kotlin
class MyModule : BaseModule() {

    private lateinit var myService: MyService

    override fun onLoad(context: ModuleContext) {
        super.onLoad(context)           // 반드시 먼저 호출
        myService = MyService(context.dataFolder)
    }

    override fun onEnable() {
        myService.start()
    }
}
```

---

## 모듈 의존성 선언

### depends — 필수 의존성

```json
{
  "id": "shop-module",
  "depends": ["economy-module"]
}
```

- `economy-module`이 먼저 로드된 후 `shop-module`이 로드됨
- `economy-module`이 없거나 로드 실패하면 `shop-module`도 로드 실패

### softDepends — 선택 의존성

```json
{
  "id": "shop-module",
  "softDepends": ["discount-module"]
}
```

- `discount-module`이 없어도 `shop-module`은 정상 로드됨
- 있으면 먼저 로드되고, ClassLoader 가시성도 허용됨
- 런타임에서 null 체크 필요

```kotlin
val discount = context.api.serviceRegistry.get(DiscountService::class)
if (discount == null) {
    logger.warn("discount-module 없음 — 할인 기능 비활성화")
    return
}
```

### 버전 범위 지정

```json
{
  "depends": [
    "economy-module:>=1.2.0",
    "auth-module:1.0.0 - 2.0.0",
    "util-module:>=2.0.0"
  ]
}
```

표현식 조합 순서: `id:버전?@loadOrder`

| 표현식 | 버전 | 필수여부 | 로드순서 |
| --- | --- | --- | --- |
| `"economy-module"` | 무관 | 필수 | 기본 |
| `"economy-module:>=1.2.0"` | 1.2.0 이상 | 필수 | 기본 |
| `"economy-module:>1.2.0"` | 1.2.0 초과 | 필수 | 기본 |
| `"economy-module:1.0.0 - 2.0.0"` | 1.0.0 ~ 2.0.0 | 필수 | 기본 |
| `"economy-module?"` | 무관 | 선택 | 기본 |
| `"economy-module:>=1.2.0?"` | 1.2.0 이상 | 선택 | 기본 |
| `"economy-module@before"` | 무관 | 필수 | 먼저 로드 |
| `"economy-module@after"` | 무관 | 필수 | 나중에 로드 |
| `"economy-module:>=1.2.0?@before"` | 1.2.0 이상 | 선택 | 먼저 로드 |
| `"economy-module!"` | 무관 | 필수 | 먼저 로드 (`@before` 축약) |

버전이 맞지 않으면 `VersionMismatch` 오류로 로드 실패합니다.

### loadBefore — 로드 순서만 제어

```json
{
  "id": "my-module",
  "loadBefore": ["other-module"]
}
```

- `my-module`이 `other-module`보다 먼저 로드됨
- `depends`와 달리 의존성 강제나 ClassLoader 가시성은 없음
- 순서만 보장하고 싶을 때 사용

### @ModuleSpec에서 선언

```kotlin
@ModuleSpec(
    id = "shop-module",
    dependencies = [
        "economy-module",           // 필수, 버전 무관
        "economy-module:>=1.2.0",   // 필수, 버전 범위
        "discount-module?"          // 선택 (softDepends)
    ]
)
class ShopModule : BaseModule()
```

`?`를 붙이면 `softDepends`와 동일하게 동작합니다.

---

## 로거 & 데이터 폴더

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
        // 로거 — 모듈 ID 접두사가 자동으로 붙음
        logger.info("서버 시작됨")
        logger.warn("설정 파일이 없습니다. 기본값을 사용합니다.")
        logger.error("예외 발생", exception)

        // 하위 접두사 로거
        val dbLogger = logger.withPrefix("DB")
        dbLogger.info("연결 시도 중...")   // [my-module/DB] 연결 시도 중...

        // 데이터 폴더 — 플러그인별로 격리된 Path
        val configFile = dataFolder.resolve("config.yml")
    }
}
```

---

## CleanupScope — 자동 리소스 정리

`onDisable` / `onUnload`에서 직접 해제 코드를 작성하는 대신,  
`cleanupScope`에 등록하면 모듈 언로드 시 **자동으로 역순(LIFO)**으로 정리됩니다.

```kotlin
class MyModule : BaseModule() {

    override fun onLoad(context: ModuleContext) {
        super.onLoad(context)

        // 1. AutoCloseable 바로 등록
        val connection = DatabaseConnection.open()
        context.cleanupScope.register(connection)

        // 2. 람다로 등록
        context.cleanupScope.onClose {
            logger.info("정리 완료")
        }

        // 3. 이름 키로 등록 — 나중에 교체하거나 즉시 해제 가능
        context.cleanupScope.register("cache", myCache)
    }

    override fun onEnable() {
        // 캐시를 새 인스턴스로 교체 (이전 것은 즉시 close() 됨)
        context.cleanupScope.register("cache", newCache)
    }

    override fun onDisable() {
        // 특정 리소스만 지금 해제
        context.cleanupScope.release("cache")
    }

    // onUnload()를 따로 구현하지 않아도 cleanupScope가 나머지를 정리해줌
}
```

---

## 서비스 등록 & 조회

### 서비스 인터페이스 + 구현체 정의

```kotlin
interface EconomyService {
    fun getBalance(playerId: String): Double
    fun deposit(playerId: String, amount: Double)
}

class EconomyServiceImpl : EconomyService {
    private val balances = mutableMapOf<String, Double>()

    override fun getBalance(playerId: String) = balances[playerId] ?: 0.0
    override fun deposit(playerId: String, amount: Double) {
        balances[playerId] = (balances[playerId] ?: 0.0) + amount
    }
}
```

### 서비스 등록 (제공 모듈)

```kotlin
class EconomyModule : BaseModule() {

    override fun onEnable() {
        context.api.serviceRegistry.register(
            EconomyService::class,
            EconomyServiceImpl(),
            owner = /* ModuleContainerView */ ...
        )
        logger.info("EconomyService 등록 완료")
    }
}
```

```kotlin
class EconomyModule : BaseModule() {

    override fun onEnable() {
	    val ctx = context as RuntimeModuleContext
	    ctx.services.register(
            EconomyService::class,
            EconomyServiceImpl()
        )
        logger.info("EconomyService 등록 완료")
    }
}
```

### 서비스 자동 등록

```kotlin
@ARCService
class EconomyService {
    private val balances = mutableMapOf<String, Double>()

    fun getBalance(playerId: String) = balances[playerId] ?: 0.0
    fun deposit(playerId: String, amount: Double) {
        balances[playerId] = (balances[playerId] ?: 0.0) + amount
    }
}
```

`@ARCService` 어노테이션이 붙어있을 경우 자동으로 등록 코드를 생성합니다.

### 서비스 조회 (소비 모듈)

```kotlin
class ShopModule : BaseModule() {

    override fun onEnable() {
	    val ctx = context as RuntimeModuleContext
      
        // 없으면 null
        val economy = context.api.serviceRegistry.get(EconomyService::class)
	    val economy = ctx.services.get(EconomyService::class)

        // 없으면 ServiceNotFoundException 던짐
        val economy = context.api.serviceRegistry.require(EconomyService::class)
	    val economy = ctx.services.require(EconomyService::class)

        // ModuleContext에서 직접 조회
        val economy = context.getService(EconomyService::class.java)
	    val economy = ctx.getService(EconomyService::class.java)
      
        economy?.deposit("player-uuid", 1000.0)
    }
}
```

---

## 커맨드 등록

### 커맨드 클래스 작성

```kotlin
@CommandSpec(
	name = "eco",
    aliases = listOf("economy"),
    permission = "my-module.eco",
    description = "경제 커맨드",
    usage = "/eco <give|take|check> <player> [amount]"
)
class EconomyCommand : ARCCommand {

    override fun execute(context: CommandContext): CommandResult {
        if (!context.sender.hasPermission("my-module.eco")) {
            return CommandResult.NoPermission()
        }

        return when (context.subCommand) {
            "give"  -> handleGive(context)
            "take"  -> handleTake(context)
            "check" -> handleCheck(context)
            else    -> CommandResult.InvalidUsage(metadata.usage)
        }
    }

    private fun handleGive(context: CommandContext): CommandResult {
        val (player, amountStr) = context.remainingArgs
            .takeIf { it.size >= 2 }
            ?: return CommandResult.InvalidUsage("/eco give <player> <amount>")

        val amount = amountStr.toDoubleOrNull()
            ?: return CommandResult.Failure("잘못된 금액: $amountStr")

        // 실제 로직 처리 ...
        context.sender.sendMessage("$player 에게 ${amount}G 지급")
        return CommandResult.Success
    }

    private fun handleTake(context: CommandContext): CommandResult { /* ... */ return CommandResult.Success }
    private fun handleCheck(context: CommandContext): CommandResult { /* ... */ return CommandResult.Success }

    override fun onTabComplete(context: CommandContext): List<String> {
        return when (context.subCommand) {
            null   -> listOf("give", "take", "check")
            "give", "take", "check" -> listOf("<player>")
            else   -> emptyList()
        }
    }
}
```

### 커맨드 등록

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
	    val ctx = context as RuntimeModuleContext
      
        context.api.commandRegistry.register(
            EconomyCommand(),
            owner = /* ModuleContainerView */...
        )
        ctx.commands.register(EconomyCommand())
    }

    // 모듈 언로드 시 commandRegistry.unregisterAllById(id)가 자동 호출됨
}
```

`@ARCCommand` 어노테이션을 커맨드 클래스에 붙일 경우, 자동으로 등록합니다.

---

## 리스너 등록

### 커스텀 이벤트 정의

```kotlin
// 일반 이벤트
class PlayerJoinModuleEvent(val playerId: String) : Event

// 취소 가능한 이벤트
class PlayerPurchaseEvent(
    val playerId: String,
    val itemId: String,
    val price: Double
) : CancellableEvent {
    override var cancelled: Boolean = false
}
```

### 리스너 클래스 작성

```kotlin
class ShopListener : EventHandler {

    override fun handle(event: Event) {
        when (event) {
            is PlayerJoinModuleEvent  -> onPlayerJoin(event)
            is PlayerPurchaseEvent    -> onPlayerPurchase(event)
        }
    }

    private fun onPlayerJoin(event: PlayerJoinModuleEvent) {
        println("${event.playerId} 접속")
    }

    private fun onPlayerPurchase(event: PlayerPurchaseEvent) {
        if (event.price > 10000.0) {
            event.cancelled = true   // 구매 취소
        }
    }
}
```

```kotlin
class TestListener() : Listener {

	@EventHandler
	fun onPlayerJoin(event: PlayerJoinEvent) {
		event.player.sendMessage("§a[TestModule] Welcome, ${event.player.name}!")
	}
}
```

`@ARCListener` 어노테이션을 리스너 클래스에 붙이면, 자동으로 등록 코드를 생성합니다.

### 리스너 등록

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
	    val ctx = context as RuntimeModuleContext
			
        val listener = ShopListener()

        // 특정 이벤트 타입만 수신
        context.api.eventManager.register(
            listener,
            PlayerJoinModuleEvent::class.java,
            PlayerPurchaseEvent::class.java
        )
        
        ctx.listeners.register(
			TestListener(),
			plugin = /* Plugin */ ...
		)

        // 언로드 시 자동 해제
        context.cleanupScope.onClose {
            context.api.eventManager.unregister(listener)
        }
    }
}
```

### 이벤트 발행

```kotlin
context.api.eventManager.post(PlayerJoinModuleEvent(playerId = "uuid-1234"))

val purchaseEvent = PlayerPurchaseEvent("uuid-1234", "diamond_sword", 5000.0)
context.api.eventManager.post(purchaseEvent)
if (!purchaseEvent.cancelled) {
    // 구매 진행
}
```

---

## 다른 모듈의 서비스 사용하기

별도 Gradle 프로젝트로 작성된 다른 모듈 플러그인의 서비스를 사용하는 경우입니다.

### Step 1. 매니페스트에 의존성 선언

```json
{
  "id": "shop-module",
  "depends": ["economy-module"]
}
```

이 선언이 없으면 ClassLoader 간 클래스 가시성이 허용되지 않아 `ClassNotFoundException`이 발생합니다.

### Step 2. 빌드 의존성 추가

제공 모듈의 API jar를 `compileOnly`로 추가합니다.

```kotlin
// build.gradle.kts
dependencies {
    compileOnly(files("libs/economy-module-api.jar"))
    // 또는 같은 Gradle 멀티 프로젝트라면:
    compileOnly(project(":economy-module"))
}
```

### Step 3. 서비스 조회

```kotlin
class ShopModule : BaseModule() {

    private lateinit var economy: EconomyService

    override fun onEnable() {
        economy = context.api.serviceRegistry.require(EconomyService::class)
        // 이제 economy를 자유롭게 사용
        logger.info("잔액: ${economy.getBalance("uuid-1234")}")
    }
}
```

소프트 의존성인 경우 (`softDepends`) 서비스가 없을 수 있으므로 null 체크를 합니다.

```kotlin
val economy = context.api.serviceRegistry.get(EconomyService::class)
if (economy == null) {
    logger.warn("EconomyService 없음 — 관련 기능 비활성화")
    return
}
```

---

## 모듈 상태 & 라이프사이클 관찰

### 다른 모듈의 상태 확인

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
        val moduleManager = context.api.moduleManager

        // 모듈 로드 여부 확인
        if (moduleManager.isLoaded("economy-module")) {
            logger.info("economy-module 로드됨")
        }

        // 상태 직접 확인
        val state = moduleManager.getState("economy-module")
        logger.info("economy-module 상태: $state")  // ENABLED, DISABLED, ...

        // 모듈 인스턴스 가져오기
        val economyModule = moduleManager.getModule("economy-module")
    }
}
```

### 라이프사이클 이벤트 구독

```kotlin
val observer = LifecycleObserver { event ->
    when (event.type) {
        LifecycleEventType.ENABLED  -> logger.info("${event.container.module.id} 활성화됨")
        LifecycleEventType.DISABLED -> logger.info("${event.container.module.id} 비활성화됨")
        LifecycleEventType.FAILED   -> logger.error("실패: ${event.cause?.message}")
        else -> {}
    }
}
```

특정 이벤트 타입만 받으려면 `FilteredLifecycleObserver`를 사용합니다.

```kotlin
val observer = object : FilteredLifecycleObserver(
    LifecycleEventType.FAILED,
    LifecycleEventType.DEPENDENCY_FAILED
) {
    override fun onAcceptedEvent(event: LifecycleEvent) {
        logger.error("모듈 장애 감지: ${event.container.module.id}")
    }
}
```

### 모듈 Hot Reload

```kotlin
val result = context.api.moduleManager.reload("my-module")

when (result) {
    is ReloadResult.Success       -> logger.info("리로드 완료 (${result.elapsedMs}ms)")
    is ReloadResult.Failure       -> logger.error("리로드 실패: ${result.error.message}")
    is ReloadResult.Rejected      -> logger.warn("리로드 거부됨: ${result.reason}")
    is ReloadResult.AlreadyReloading -> logger.warn("이미 리로드 중")
    else -> {}
}
```

---

## Hot Reload 힌트

모듈이 `ModuleReloadHint`를 구현하면 Hot Reload 시 동작을 제어할 수 있습니다.

### 상태 보존 (StatefulReload)

```kotlin
class MyModule : BaseModule(), ModuleReloadHint.StatefulReload {

    private var playerData: Map<String, Int> = emptyMap()

    override fun captureState(): Map<String, Any?> {
        return mapOf("playerData" to playerData)
    }

    @Suppress("UNCHECKED_CAST")
    override fun restoreState(state: Map<String, Any?>) {
        playerData = state["playerData"] as? Map<String, Int> ?: emptyMap()
        logger.info("상태 복원 완료: ${playerData.size}명 데이터")
    }
}
```

### 상태 없이 단순 재시작 (StatelessReload)

```kotlin
class MyModule : BaseModule(), ModuleReloadHint.StatelessReload
// 프레임워크가 단순히 unload → load 수행
```

이 상태에서는 굳이 모듈 리로드 힌트를 작성할 필요 없습니다.

### 조건부 리로드 허용 (ConditionalReload)

```kotlin
class MyModule : BaseModule(), ModuleReloadHint.ConditionalReload {

    private var hasActiveTransaction = false

    override fun canReload(): Boolean = !hasActiveTransaction

    override fun rejectReason(): String = "진행 중인 트랜잭션이 있어 리로드할 수 없습니다."
}
```

---

## 의존성 주입 (DI)

ARCCore DI는 두 가지 경로로 동작합니다.

| 경로 | 어노테이션 | 동작 |
| --- | --- | --- |
| Bootstrap DI | `@ARCCommand` / `@ARCListener` | 생성자 파라미터를 `ServiceRegistry`에서 자동 주입 |
| Generated DI | `@ArcComponent` | KSP가 팩토리를 생성, 모듈 내 객체 그래프 구성 |

---

### Bootstrap DI — 커맨드 / 리스너 자동 주입

`@ARCCommand` 또는 `@ARCListener`가 붙은 클래스의 생성자 파라미터는  
KSP가 생성한 `ArcBootstrap.register()`에서 `ServiceRegistry`를 통해 자동 주입됩니다.

```kotlin
// 1. 서비스를 ServiceRegistry에 등록
class EconomyModule : BaseModule() {
    override fun onEnable() {
        context.api.serviceRegistry.register(
            EconomyService::class,
            EconomyServiceImpl(),
            owner = ...
        )
    }
}
```

```kotlin
// 2. 커맨드 생성자에서 서비스를 선언 — 자동으로 주입됨
@CommandSpec(name = "balance", description = "잔액 확인")
@ARCCommand
class BalanceCommand(
    private val economy: EconomyService   // non-nullable → require()
) : ARCCommand {

    override fun execute(context: CommandContext): CommandResult {
        val balance = economy.getBalance(context.sender.name)
        context.sender.sendMessage("잔액: ${balance}G")
        return CommandResult.Success
    }
}
```

```kotlin
// 3. nullable이면 get() (없어도 로드됨), non-nullable이면 require() (없으면 예외)
@ARCListener
class ShopListener(
    private val economy: EconomyService,          // 필수
    private val discount: DiscountService?        // 선택
) : Listener {
    @EventHandler
    fun onPurchase(event: PlayerInteractEvent) {
        val price = discount?.apply(1000.0) ?: 1000.0
        economy.deposit(event.player.uniqueId.toString(), -price)
    }
}
```

KSP가 생성하는 코드는 다음과 같습니다.

```kotlin
// KSP 생성 코드 (직접 작성 불필요)
context.commands.register(BalanceCommand(
    context.services.require(EconomyService::class)
))
context.listeners.register(ShopListener(
    context.services.require(EconomyService::class),
    context.services.get(DiscountService::class)
), plugin)
```

---

### Generated DI — 주입 경로

`DefaultDIContainer.resolve()`는 아래 순서로 타입을 탐색합니다.  
앞 경로에서 찾으면 이후 경로는 탐색하지 않습니다.

---

#### 경로 1 — `@ArcComponent` (권장)

KSP가 `Generated{ClassName}Factory`를 생성합니다.  
`GeneratedObjectGraph`가 팩토리를 통해 **리플렉션 없이** 인스턴스를 만듭니다.

```kotlin
@ArcComponent
class PlayerRepository @Inject constructor(
    private val db: DatabaseService
)

@ArcComponent
@ArcSingleton                    // 모듈 로드 사이클당 1개, 없으면 매번 새로 생성
class EconomyService @Inject constructor(
    private val repo: PlayerRepository
) {
    fun getBalance(id: String): Double = TODO()
}
```

생성자가 1개면 `@Inject` 생략 가능합니다.

```kotlin
@ArcComponent
class MyService(private val dep: OtherService)
```

---

#### 경로 2 — `bindModuleInstance()` 수동 등록(비권장)

모듈 스코프로 인스턴스를 직접 바인딩합니다.

```kotlin
container.bindModuleInstance("my-module", DatabaseService::class, DatabaseService())
```

---

#### 경로 3 — `bindModule()` 수동 등록(비권장)

모듈 스코프로 프로바이더를 바인딩합니다. 처음 resolve 시 한 번 생성하고 이후 캐싱됩니다.

```kotlin
container.bindModule("my-module", DatabaseService::class, object : InstanceProvider<DatabaseService> {
    override val scope = Scope.Module
    override fun provide(context: ResolutionContext) = DatabaseService()
})
```

---

#### 경로 4 — `bindInstance()` 수동 등록(비권장)

전역 싱글톤 인스턴스를 직접 바인딩합니다.

```kotlin
container.bindInstance(DatabaseService::class, DatabaseService())
```

---

#### 경로 5 — `bindSingleton()` 수동 등록(비권장)

전역 싱글톤 프로바이더를 바인딩합니다. 처음 resolve 시 생성하고 이후 캐싱됩니다.

```kotlin
container.bindSingleton(DatabaseService::class, object : InstanceProvider<DatabaseService> {
    override val scope = Scope.Singleton
    override fun provide(context: ResolutionContext) = DatabaseService()
})
```

---

#### 경로 6 — `ServiceRegistry` 등록(권장)

`ServiceRegistry`에 등록된 타입도 DI 컨테이너에서 resolve됩니다.  
`@ARCService` + `ServiceRegistry` 등록 조합이 대표적인 사례입니다.

```kotlin
@ARCService
class NotificationService {
    fun send(message: String) = println(message)
}

// 모듈에서 ServiceRegistry에 등록
class MyModule : BaseModule() {
    override fun onEnable() {
        context.api.serviceRegistry.register(
            NotificationService::class,
            NotificationService(),
            owner = ...
        )
    }
}

// @ArcComponent 클래스가 NotificationService를 주입받을 수 있음 (경로 6)
@ArcComponent
class AlertManager @Inject constructor(
    private val notification: NotificationService
) {
    fun alert(msg: String) = notification.send("[ALERT] $msg")
}
```

`@ARCService`만 붙이고 `ServiceRegistry`에 등록하지 않으면  
어떤 경로도 찾지 못해 `MissingDependencyException`이 발생합니다.

---

#### 경로 7 — `ConstructorInjector` 리플렉션 폴백(권장)

1~6번 경로에서 찾지 못하면 리플렉션으로 시도합니다.  
`@Inject` 생성자가 있거나 생성자가 정확히 1개면 동작합니다.

```kotlin
// @Inject 생성자
class ReportService @Inject constructor(
    private val db: DatabaseService
)

// 단일 생성자 (어노테이션 없음)
class CacheService(
    private val db: DatabaseService
)

// 생성자 2개 이상 + @Inject 없음 → InvalidInjectableException
class BrokenService(val a: String) {
    constructor() : this("default")   // @Inject 없으면 어떤 생성자를 쓸지 알 수 없음
}
```

---

### 수동 바인딩 — `Container`(비권장)

`@ArcComponent` 없이 인스턴스를 직접 등록하고 꺼내는 방법입니다.

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
        val container = context.api.container

        // 등록
        container.register(MyConfig::class.java, MyConfig(maxPlayers = 100))
        container.registerSingleton(MyCache::class.java, MyCache())

        // 조회
        val config = container.get(MyConfig::class.java)
        val cache = container.getOrNull(MyCache::class.java)

        // 존재 확인
        if (container.has(MyConfig::class.java)) {
            logger.info("설정 로드됨")
        }
    }
}
```

`container`를 사용한 모듈 간의 의존성 주입은 일반적으로 권장하지 않습니다.
`@ARCComponent`와 `@Inject`를 이용하세요.

---

## 레지스트리

전역 키-값 저장소로, 타입 안전하게 임의 객체를 공유할 수 있습니다.

```kotlin
class MyModule : BaseModule() {

    override fun onEnable() {
        val registry = context.api.registry

        // 등록
        val key = RegistryKey.of("my-module", "config")
        registry.register(key, MyConfig(maxPlayers = 100))

        // 조회
        val config = registry.get<MyConfig>(key)
        logger.info("최대 플레이어: ${config?.maxPlayers}")

        // 존재 여부 확인
        if (registry.has(key)) {
            logger.info("설정 로드됨")
        }
    }
}
```

`RegistryKey`는 `"namespace:name"` 문자열로도 파싱할 수 있습니다.

```kotlin
val key = RegistryKey.parse("my-module:config")
```
