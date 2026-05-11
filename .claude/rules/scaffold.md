---
description: 새 Aggregate/UseCase 생성 템플릿, Inside-Out 워크플로우, DDL 패턴
alwaysApply: true
---

# 스캐폴딩 규칙

## 계층별 Java/Spring 기능 사용 범위

| 기능 | Domain | Application | Adapter | Configuration |
|------|:------:|:-----------:|:-------:|:-------------:|
| `record` (VO, DTO) | O | O | O | O |
| `sealed interface/class` | O | O | O | O |
| `ScopedValue` | **X** | O (읽기) | O (바인딩) | O |
| `Objects.requireNonNull` | O | O | O | O |
| `Clock` / `Instant.now()` | **X** | O (Clock 주입) | O | O |
| Spring 어노테이션 | **X** | **X** | O | O |
| jOOQ | **X** | **X** | O | **X** |
| Jackson 어노테이션 | **X** | **X** | O | O |
| JSpecify (`@Nullable`) | **X** | **X** | O | O |
| `@Transactional` | **X** | **X** | **X** | O (TX 프록시) |
| Virtual Threads | — | — | O (자동) | O (설정) |

- **O**: 사용 가능
- **X**: 사용 금지
- **—**: 해당 없음 (런타임에서 자동 적용)

---

## Inside-Out 개발 원칙

MUST: 항상 domain 모듈부터 시작하고 바깥으로 나간다.
MUST NOT: Controller나 DB 스키마부터 시작하지 않는다.

개발 순서:
1. `domain/model/` — Aggregate Root + VO
2. `domain/event/` — Domain Event (sealed interface)
3. `domain/service/` — Domain Service (필요 시)
4. `domain/exception/` — Domain Exception (sealed class)
5. `application/port/input/` — Input Port (UseCase 인터페이스)
6. `application/port/output/` — Output Port (Load/Save/Query 분리)
7. `application/dto/` — Command, Query, Result record
8. `application/service/` — UseCase 구현체 Service
9. `adapter/output/` — PersistenceAdapter, QueryAdapter + Mapper
10. `adapter/input/` — Controller + Request/Response DTO
11. `configuration/` — BeanConfiguration (Bean + TX 프록시)
12. `resources/db/migration/` — `V{n}__create_{subject}.sql`

## 식별자 전략 (UUIDv7)

MUST: Aggregate Root의 식별자는 UUIDv7을 사용한다.
MUST NOT: `UUID.randomUUID()` (UUIDv4) — B-Tree 인덱스 페이지 분할로 성능 저하.
MUST NOT: Auto Increment — 분산 환경에서 충돌 위험.
MUST: UUIDv7 생성 로직은 `UUIDv7` 유틸리티 클래스에 격리한다. VO 내부에 생성 로직 중복 금지.

> 근거: ADR-0011

**UUIDv7 유틸리티 클래스 (domain 모듈 공용)**

```java
// io/github/ppzxc/boilerplate/{bc}/domain/model/UUIDv7.java
// 순수 Java — 외부 의존 없음, RFC 9562 §6.2 Method 1 (monotonic counter)
public final class UUIDv7 {
    private UUIDv7() {}

    private static final class State {
        final long timestamp;
        final int counter;
        State(long timestamp, int counter) {
            this.timestamp = timestamp;
            this.counter = counter;
        }
    }

    private static final AtomicReference<State> STATE =
        new AtomicReference<>(new State(0L, 0));

    public static UUID generate() {
        State prev, next;
        do {
            prev = STATE.get();
            long ts = System.currentTimeMillis();
            long newTs = prev.timestamp;
            int newCnt = prev.counter;
            if (ts > prev.timestamp) {
                newTs = ts;
                newCnt = ThreadLocalRandom.current().nextInt(0x1000);
            } else {
                newCnt = prev.counter + 1;
            }
            if (newCnt >= 0x1000) {
                // counter overflow — 다음 ms까지 대기
                while (System.currentTimeMillis() == prev.timestamp) {
                    Thread.onSpinWait();
                }
                continue;
            }
            next = new State(newTs, newCnt);
        } while (!STATE.compareAndSet(prev, next));

        long msb = ((next.timestamp & 0x0000_FFFF_FFFF_FFFFL) << 16)
                 | (0x7L << 12)
                 | (long) next.counter;
        long lsb = (0b10L << 62)
                 | (ThreadLocalRandom.current().nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);
        return new UUID(msb, lsb);
    }
}
```

설계 선택 이유:
- `AtomicReference<State>` CAS: timestamp + counter 원자적 갱신 → race condition 방지
- `ThreadLocalRandom`: Virtual Thread 친화적 고성능 난수 (SecureRandom의 초기화 오버헤드 없음)
- monotonic counter: 동일 ms 내 순서 보장, overflow(>= 0x1000) 시 next ms 대기
- clock rollback: ts <= prev.timestamp이면 counter 증가 → 역행 방지

**Id VO 템플릿 (UUIDv7 사용)**

```java
public record {Subject}Id(UUID value) {
    public {Subject}Id {
        Objects.requireNonNull(value, "{Subject}Id must not be null");
    }

    public static {Subject}Id generate() {
        return new {Subject}Id(UUIDv7.generate());
    }
}
```

## 새 Aggregate 생성 체크리스트

### Phase 1: Domain

순서를 지킨다. 각 항목은 다음 항목의 선행 조건이다.

- [ ] `{Subject}Id` Value Object (record, UUIDv7 팩토리 메서드 포함)
- [ ] 도메인 의미 VO들 (`Email`, `UserName` 등, 전부 record)
- [ ] `{Subject}Status` enum (상태가 있는 경우)
- [ ] `{Subject}Event` sealed interface + 구현 record들
- [ ] `{Subject}Exception` sealed class + 구현 클래스들
- [ ] Aggregate Root `{Subject}` 클래스
  - `create()` static 팩토리 — 신규 생성
  - `reconstitute()` static 팩토리 — DB 복원
  - 행위 메서드들 (비즈니스 로직 전부 여기)
  - `pullDomainEvents()` — 이벤트 수거 후 비움

### Phase 2: Application

- [ ] `Load{Subject}Port` (인터페이스, findById 등)
- [ ] `Save{Subject}Port` (인터페이스, save 포함)
- [ ] `{Subject}QueryPort` (인터페이스, 복잡 조회용)
- [ ] Command record들 (`{Verb}{Subject}Command`)
- [ ] Query record들 (`{Verb}{Subject}Query`)
- [ ] Result record들 (`{Subject}Result`, `{Subject}Detail` 등)
- [ ] UseCase 인터페이스들 (`{Verb}{Subject}UseCase`)
- [ ] UseCase 구현체 Service들 (`{Verb}{Subject}Service`)

### Phase 3: Adapter

- [ ] jOOQ PersistenceMapper — Domain ↔ jOOQ Record 수동 변환
- [ ] `{Subject}PersistenceAdapter` — Load/Save Port 구현
  - `save()`: `aggregate.pullDomainEvents()`로 이벤트 수거 → ApplicationEventPublisher 발행
  - `reconstitute()` 호출 (DB 로드 시)
- [ ] `{Subject}QueryAdapter` — Query Port 구현
- [ ] `{Subject}Controller` — REST API, Input Port만 의존
  - Request/Response DTO (record)
  - `@PostMapping`, `@GetMapping` 등

### Phase 4: Configuration

- [ ] `{Bc}BeanConfiguration` — Bean 등록
  - 모든 UseCase Service Bean 등록
  - TX 프록시 등록 (`createTxProxy` 헬퍼 메서드 사용)
  - `// --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---` anchor 포함

### Phase 5: DDL

- [ ] `V{n}__create_{subject_snake}.sql` — Flyway 마이그레이션

## 파일별 코드 템플릿

### Domain Event 템플릿

```java
// {Subject}Event.java — sealed interface로 그룹화 (D-13)
public sealed interface {Subject}Event extends DomainEvent
    permits {Subject}CreatedEvent /*, ... */ {}

// {Subject}CreatedEvent.java — 5필드 필수
public record {Subject}CreatedEvent(
    UUID eventId,           // UUIDv7, VO 래핑 아님
    String eventType,
    UUID aggregateId,       // VO 래핑 아님
    Instant occurredAt,
    long aggregateVersion
    // 페이로드 필드
) implements {Subject}Event {}
```

### Aggregate Root 템플릿

```java
public final class {Subject} {
    private final {Subject}Id id;
    private {Subject}Status status;
    private final long version;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private {Subject}(/* all fields */) {
        this.id = Objects.requireNonNull(id);
        // ...
    }

    public static {Subject} create(/* params, Instant now */) {
        var id = {Subject}Id.generate();
        var entity = new {Subject}(/* ... */);
        entity.registerEvent(new {Subject}CreatedEvent(
            {Subject}Id.generate().value(), // eventId — UUIDv7
            "{Subject}CreatedEvent",
            id.value(), now, 0L /*, payload */));
        return entity;
    }

    public static {Subject} reconstitute(/* all fields + version */) {
        return new {Subject}(/* ... */);
    }

    private void registerEvent(DomainEvent e) { domainEvents.add(e); }

    public List<DomainEvent> pullDomainEvents() {
        var e = List.copyOf(domainEvents);
        domainEvents.clear();
        return e;
    }

    public {Subject}Id id() { return id; }
    public long version() { return version; }
}
```

### 내부 Entity 템플릿 (Credential 패턴)

Aggregate 내부의 하위 Entity. 외부에서 직접 접근 금지, package-private `create()`/`reconstitute()`.

```java
// {Subject} Aggregate 내부 Entity — 외부에서 직접 접근 금지
public final class {ChildEntity} {
    private final {ChildEntity}Id id;
    private final {ChildEntity}Type type;
    private final Instant createdAt;

    private {ChildEntity}({ChildEntity}Id id, {ChildEntity}Type type, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    // package-private — Aggregate Root를 통해서만 생성
    static {ChildEntity} create({ChildEntity}Type type, Instant now) {
        return new {ChildEntity}({ChildEntity}Id.generate(), type, now);
    }

    static {ChildEntity} reconstitute({ChildEntity}Id id, {ChildEntity}Type type, Instant createdAt) {
        return new {ChildEntity}(id, type, createdAt);
    }

    public {ChildEntity}Id id() { return id; }
    public {ChildEntity}Type type() { return type; }
}
```

- MUST: 내부 Entity의 `create()`, `reconstitute()`는 package-private. Aggregate Root를 통해서만 생성.
- MUST: 외부에서 내부 Entity를 직접 생성하는 것을 금지한다.

### PersistenceAdapter 템플릿 (AD-3, AD-7)

```java
class {Subject}PersistenceAdapter implements Load{Subject}Port, Save{Subject}Port {
    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher; // Modulith

    @Override
    public Optional<{Subject}> findById({Subject}Id id) {
        return dsl.selectFrom({SUBJECT})
            .where({SUBJECT}.ID.eq(id.value()))
            .fetchOptional(r -> {Subject}PersistenceMapper.toDomain(r));
    }

    @Override
    public void save({Subject} entity) {
        int affected = dsl.update({SUBJECT})
            .set({SUBJECT}.STATUS, entity.status().name())
            .set({SUBJECT}.VERSION, entity.version() + 1)
            .where({SUBJECT}.ID.eq(entity.id().value()))
            .and({SUBJECT}.VERSION.eq(entity.version()))  // AD-7: WHERE version = ?
            .execute();
        if (affected == 0) {
            throw new OptimisticLockException(entity.id()); // AD-7
        }
        // 이벤트 수거 → Modulith 발행 (AD-3: 같은 TX)
        entity.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
```

### BeanConfiguration 템플릿 (A-4, T-1)

```java
@Configuration
class {Context}BeanConfiguration {

    @Bean
    Clock {context}Clock() {
        return Clock.systemUTC();
    }

    // --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---

    @Bean
    Create{Subject}UseCase create{Subject}UseCase(
            {Subject}PersistenceAdapter adapter,
            Clock clock,
            PlatformTransactionManager txManager) {
        var service = new Create{Subject}Service(adapter, clock);
        return createTxProxy(service, Create{Subject}UseCase.class, txManager);
    }

    private <T> T createTxProxy(Object target, Class<T> iface,
                                PlatformTransactionManager txManager) {
        var source = new MatchAlwaysTransactionAttributeSource();
        var props = new Properties();
        props.setProperty("*", "PROPAGATION_REQUIRED");
        source.setProperties(props);
        var interceptor = new TransactionInterceptor(txManager, source);
        var factory = new org.springframework.aop.framework.ProxyFactory(target);
        factory.addInterface(iface);
        factory.addAdvice(interceptor);
        return iface.cast(factory.getProxy());
    }
}
```

> **Anchor Comment**: 새 UseCase Bean 추가 시 `// --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---` 바로 **아래에** 삽입한다. 기존 코드를 건드리지 않는다.

### EventTranslator 템플릿 (Domain → Integration)

Configuration 모듈에서 Domain Event를 Integration Event로 변환한다.

```java
// {bc}-configuration 모듈
@Component
class {Bc}EventTranslator {
    private final ApplicationEventPublisher publisher;

    {Bc}EventTranslator(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener
    void on({Subject}CreatedEvent event) {
        publisher.publishEvent(new {Subject}CreatedIntegrationEvent(
            event.aggregateId(),
            event.occurredAt()
        ));
    }
    // --- AI_ANCHOR: ADD_NEW_EVENT_TRANSLATION_HERE ---
}
```

- MUST: EventTranslator는 Configuration 모듈에 위치한다.
- MUST: `@TransactionalEventListener`로 Domain Event를 수신하고 Integration Event로 재발행한다.

### BC 내 Aggregate 간 이벤트 기반 분리 템플릿

같은 BC 내 Aggregate 간 통신은 Domain Event를 직접 사용한다 (Integration Event 불필요).

```java
// {bc}-adapter-input-event — 같은 BC 내 Aggregate 간
@Component
class {Subject}EventHandler {
    private final {Verb}{Target}UseCase useCase;

    @ApplicationModuleListener
    void on({Subject}{Action}Event event) {
        useCase.execute(new {Verb}{Target}Command(
            event.aggregateId().toString()
        ));
    }
}
```

- MUST: 1 TX = 1 Aggregate. 복수 Aggregate 변경이 필요하면 이벤트 기반으로 분리한다.
- MUST: 이벤트 핸들러는 `adapter-input-event` 모듈에 위치한다.

### Cross-BC Integration Event 수신 패턴 (adapter-input-event)

다른 BC에서 발행한 Integration Event를 수신하는 핸들러 템플릿.

```java
// {bc}-adapter-input-event 모듈
@Component
class {Source}{Subject}EventHandler {
    private final {Verb}{Target}UseCase useCase;

    {Source}{Subject}EventHandler({Verb}{Target}UseCase useCase) {
        this.useCase = useCase;
    }

    @ApplicationModuleListener
    void on({Subject}{Action}IntegrationEvent event) {
        useCase.execute(new {Verb}{Target}Command(
            event.userId().toString(),
            event.occurredAt().toString()
        ));
    }
}
```

- MUST: BC 간 통신은 `@ApplicationModuleListener`로 Integration Event(shared-event 모듈)를 수신한다.
- MUST: 핸들러는 UseCase를 통해 비즈니스 처리한다. 핸들러에서 직접 Port를 호출하지 않는다.

### BeanRegistrar 대안 (UseCase 10개 이상)

UseCase가 10개 이상으로 늘어나 BeanConfiguration이 비대해지면, Spring Boot 4의 `BeanRegistrar` API로 TX 프록시를 일괄 등록하는 방식을 전환을 고려한다.

- SHOULD: UseCase 10개 미만에서는 수동 Bean + TX 프록시 등록(위 템플릿)을 유지한다.
- SHOULD: UseCase 10개 이상에서는 `BeanRegistrar`로 전환을 검토한다.
- MUST: 전환 시에도 TX 경계는 Configuration 계층이 담당한다는 원칙(A-4, T-1)은 변하지 않는다.

```java
// BeanRegistrar — UseCase 대량 등록 패턴 (Spring Boot 4+)
class UseCaseBeanRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry) {
        registry.registerBean("createUserUseCase", CreateUserUseCase.class, spec -> {
            spec.supplier(ctx -> {
                var service = new CreateUserService(
                    ctx.bean(UserPersistenceAdapter.class),
                    ctx.bean(Clock.class));
                return createTxProxy(service, CreateUserUseCase.class,
                    ctx.bean(PlatformTransactionManager.class));
            });
        });
        // 추가 UseCase 등록...
    }
}
```

### DDL 템플릿

```sql
CREATE TABLE {subject} (
    id          UUID          NOT NULL PRIMARY KEY,
    status      VARCHAR(20)   NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

### DDL Backward Compatibility

MUST: DDL 변경은 Backward Compatible이어야 한다. Rolling Update 중 구/신 버전 앱이 동시에 실행된다.

| 변경 유형 | 안전 여부 | 방법 |
|-----------|----------|------|
| 컬럼 추가 (NOT NULL + DEFAULT) | 안전 | 단일 마이그레이션 |
| 컬럼 삭제 | **위험** | 3단계 필수 |
| 컬럼 이름 변경 | **위험** | 3단계 필수 |
| 테이블 추가 | 안전 | 단일 마이그레이션 |

MUST NOT: `RENAME COLUMN` 또는 `DROP COLUMN`을 단일 마이그레이션에서 수행한다.

MUST: 컬럼 삭제/이름 변경은 **3단계 마이그레이션**을 따른다:

```
Phase 1: V{n}__add_new_column.sql
  → 새 컬럼 추가, 기존 컬럼 유지, 앱 코드는 양쪽 모두 기록

Phase 2: 앱 코드 전환 (별도 릴리스)
  → 새 컬럼만 읽기/쓰기, 구 컬럼은 더 이상 사용 안 함

Phase 3: V{n+1}__drop_old_column.sql
  → 구 컬럼 삭제 (구 버전 앱이 완전히 내려간 후)
```

---

## 신규 BC 모듈 초기화 체크리스트

새 BC를 추가할 때 아래 순서로 Gradle 모듈을 생성하고 등록한다.

### Step 0: DDD 5단계 모델링 게이트

MUST: Gradle 모듈 생성(Step 1) 전에 반드시 `strategic-design.md Step 1~5`를 완료한다.

체크리스트:
- [ ] `docs/ddd/bounded-context-{bc}.md` §1 Domain 작성됨 (비즈니스 범위, Subdomain 유형, DDD 깊이)
- [ ] `docs/ddd/bounded-context-{bc}.md` §2 UL 표 작성됨 (다른 BC와 용어 충돌 없음)
- [ ] `docs/ddd/bounded-context-{bc}.md` §3 Entity & VO 식별됨 (Entity vs VO 결정 기준 표 적용)
- [ ] `docs/ddd/bounded-context-{bc}.md` §4 Aggregate & Root 설계됨 (4역할 확인, 1 TX = 1 Aggregate 설계)
- [ ] `docs/ddd/bounded-context-{bc}.md` §5 BC 경계 + 통합 패턴 결정됨
- [ ] `docs/ddd/context-map.md` §1 Subdomain 표 + §2 다이어그램 + §5 모듈 매핑 갱신됨
- [ ] (필요 시) ADR 작성됨

MUST: 위 7개 항목이 모두 충족되어야 Step 1(디렉토리 구조 생성)로 진행한다.

---

### Step 1: 디렉토리 구조

```
boilerplate/{bc}/
  boilerplate-{bc}-domain/
    build.gradle.kts
    src/main/java/io/github/ppzxc/boilerplate/{bc}/domain/
      package-info.java
  boilerplate-{bc}-application/
    build.gradle.kts
    src/main/java/io/github/ppzxc/boilerplate/{bc}/application/
      package-info.java
  boilerplate-{bc}-configuration/
    build.gradle.kts
  boilerplate-{bc}-adapter-input-api/        (HTTP API 필요 시)
    build.gradle.kts
  boilerplate-{bc}-adapter-output-persist/
    build.gradle.kts
```

### Step 2: 각 모듈 build.gradle.kts 내용

```kotlin
// boilerplate-{bc}-domain/build.gradle.kts
label("java")
label("coverage-gate")
// dependencies 블록 없음 — D-1 외부 의존 제로

// boilerplate-{bc}-application/build.gradle.kts
label("java")
label("coverage-gate")
dependencies {
    implementation(project(":boilerplate-{bc}-domain"))
}

// boilerplate-{bc}-adapter-input-api/build.gradle.kts
label("java", "spring")
dependencies {
    implementation(project(":boilerplate-{bc}-application"))
}

// boilerplate-{bc}-adapter-output-persist/build.gradle.kts
label("java", "spring", "jooq")
dependencies {
    implementation(project(":boilerplate-{bc}-application"))
    implementation(project(":boilerplate-{bc}-domain"))
}

// boilerplate-{bc}-configuration/build.gradle.kts
label("java", "spring")
dependencies {
    implementation(project(":boilerplate-{bc}-domain"))
    implementation(project(":boilerplate-{bc}-application"))
    implementation(project(":boilerplate-{bc}-adapter-input-api"))    // 필요 시
    implementation(project(":boilerplate-{bc}-adapter-output-persist"))
}
```

### Step 3: settings.gradle.kts 등록

```kotlin
// ── {BC명} BC ──────────────────────────────────────────────────────────
module(name = ":boilerplate-{bc}-domain",
       path = "boilerplate/{bc}/boilerplate-{bc}-domain")
module(name = ":boilerplate-{bc}-application",
       path = "boilerplate/{bc}/boilerplate-{bc}-application")
module(name = ":boilerplate-{bc}-configuration",
       path = "boilerplate/{bc}/boilerplate-{bc}-configuration")
module(name = ":boilerplate-{bc}-adapter-input-api",
       path = "boilerplate/{bc}/boilerplate-{bc}-adapter-input-api")
module(name = ":boilerplate-{bc}-adapter-output-persist",
       path = "boilerplate/{bc}/boilerplate-{bc}-adapter-output-persist")
```

### Step 4: boilerplate-boot/build.gradle.kts에 configuration 추가

```kotlin
dependencies {
    // 기존 BC configuration 의존성들...
    implementation(project(":boilerplate-{bc}-configuration"))
}
```

### Step 5: package-info.java 생성

```java
// domain
/** {BC명} 도메인 레이어 — 순수 Java, Spring/JSpecify 금지. */
package io.github.ppzxc.boilerplate.{bc}.domain;

// application
/** {BC명} 애플리케이션 레이어 — 순수 Java, Spring/JSpecify 금지. */
package io.github.ppzxc.boilerplate.{bc}.application;
```

### Step 6: 빌드 확인

```bash
./gradlew :boilerplate-{bc}-domain:compileJava --no-daemon
```
Expected: BUILD SUCCESSFUL

### Step 7: `docs/ddd/` 산출물 갱신 (MUST)

새 BC 추가 시 다음 3개 파일을 모두 갱신한다 (`context-map-pointer.md` §4 참조):

- [ ] `docs/ddd/bounded-context-{bc}.md` — 신규 파일 작성 (5섹션: §1 Domain, §2 UL, §3 Entity·VO, §4 Aggregate, §5 BC 경계). `bounded-context-identity.md` 구조 참조.
- [ ] `docs/ddd/context-map.md` — §1 Subdomain 분류 표에 행 추가, §2 다이어그램에 BC + 관계 추가, §5 모듈 매핑에 Gradle 모듈 행 추가
- [ ] `docs/ddd/strategic-design-changelog.md` — 날짜, 변경 유형, 내용 기록

---

## 새 UseCase 추가 체크리스트

1. `{Verb}{Subject}Command` 또는 `{Verb}{Subject}Query` record 추가 (self-validation 포함)
2. `{Verb}{Subject}UseCase` 인터페이스 추가
3. `{Verb}{Subject}Service` 구현체 추가
4. `BeanConfiguration`의 `// --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---` 바로 아래에 Bean + TX 프록시 추가
5. (필요 시) Domain에 행위 메서드 추가 + 이벤트 추가 → `{Subject}Event` permits 갱신

## 전체 파일 생성 순서 (의존 그래프)

```
[Phase 1: Domain]
{Subject}Id.java (VO, record)
    ↓
기타 VO들 (record)
    ↓
{Subject}Status.java (enum)
    ↓
{Subject}Event.java (sealed interface)
    ↓
{Subject}CreatedEvent.java (record, 5필드)
    ↓
{Subject}Exception.java (sealed class)
    ↓
하위 Exception들 (final class)
    ↓
{Subject}.java (Aggregate Root)

[Phase 2: Application — Phase 1 완료 후]
Load{Subject}Port.java
Save{Subject}Port.java
{Subject}QueryPort.java
    ↓
Create{Subject}Command.java (원시 타입 record)
{Subject}Result.java (record)
    ↓
Create{Subject}UseCase.java
    ↓
Create{Subject}Service.java

[Phase 3: Adapter — Phase 2 완료 후]
{Subject}PersistenceMapper.java
    ↓
{Subject}PersistenceAdapter.java
{Subject}QueryAdapter.java
    ↓
{Subject}Controller.java (Domain 비의존!)

[Phase 4: Configuration]
{Bc}BeanConfiguration.java

[Phase 5: DDL]
V{n}__create_{subject}.sql
```

---

## 신규 프로젝트 생성 9단계 템플릿

**입력**: `{project}` (프로젝트명), `{context}` (첫 번째 BC), `{base-package}` (루트 패키지)

| Step | 내용 | 참조 |
|------|------|------|
| 1 | `settings.gradle.kts` 생성 — module() 함수, 5개 BC 모듈 등록 | modulith.md §2 |
| 2 | 루트 `build.gradle.kts` + `libs.versions.toml` 생성 | modulith.md §5 |
| 3 | `{context}` BC 5개 모듈 디렉토리 트리 생성 | 위 §신규 BC 모듈 초기화 체크리스트 Step 1 |
| 4 | 각 모듈 `build.gradle.kts` 생성 (label + dependencies) | 위 §신규 BC 모듈 초기화 체크리스트 Step 2 |
| 5 | `{context}-domain`에 DomainEvent 인터페이스 생성 | 위 §Domain Event 템플릿 |
| 6 | `shared-event` 모듈 생성 — Integration Event record | modulith.md §6 |
| 7 | `boot-api` 모듈 생성 — `@Modulithic`, `application.yml` | modulith.md §2 |
| 8 | Docker Compose 생성 (PostgreSQL + Grafana LGTM) | — |
| 9 | Flyway 초기 DDL + `event_publication` 테이블 | 위 §DDL 템플릿 |

- MUST: Step 1~9 순서를 지킨다. `./gradlew build`로 검증 후 첫 번째 Aggregate + UseCase 생성.

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호에 해당하는 `docs/decisions/` 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
