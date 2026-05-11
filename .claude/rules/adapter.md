---
paths: "**/adapter/**"
---

# Adapter Layer Rules

어댑터 계층 규칙 — `**/adapter/**` 경로에 자동 적용.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 경계 규칙

### AD-1 Inbound Adapter → Domain 직접 참조 금지

- MUST NOT: Inbound Adapter(`adapter-input-*`)에서 `domain` 패키지의 클래스를 직접 import한다.
- MUST: Inbound Adapter는 Application Port(Input Port 인터페이스)에만 의존한다.
- 의존성 허용: `boilerplate-{bc}-application`만 허용.

> 근거: ADR-0002

```java
// ❌ BAD — Inbound Adapter가 Domain 직접 참조
import io.github.ppzxc.boilerplate.identity.domain.model.User; // 금지

// ✅ GOOD — Application Port(UseCase 인터페이스)에만 의존
@RestController
public class UserController {
    private final RegisterUserUseCase registerUserUseCase;  // Input Port
    private final ListUsersUseCase listUsersUseCase;        // Input Port
    // domain 패키지 import 없음
}
```

### AD-2 Adapter 간 직접 참조 금지

- MUST NOT: Adapter 모듈에서 다른 Adapter 모듈을 직접 참조한다.
- 금지 예: `adapter-input-api`에서 `adapter-output-persist` 직접 import.
- MUST: Adapter 간 소통이 필요하면 Application Port를 통한다.

### T-2 Controller에서 TX 시작 금지

- MUST NOT: Controller(Inbound Adapter)에서 트랜잭션을 시작한다.
- MUST NOT: Controller 클래스/메서드에 `@Transactional`을 부착한다.
- TX 경계는 UseCase(T-1) — Configuration에서 프록시로 적용.

### T-3 Port 내부에서 독립 TX 시작 금지

- MUST NOT: Port 구현체(Outbound Adapter) 내부에서 독립적인 트랜잭션을 시작한다.
- MUST NOT: Port 구현체에 `@Transactional(propagation = REQUIRES_NEW)` 등을 사용한다.
- MUST: Port 구현체는 호출자(UseCase)의 트랜잭션에 참여한다.

---

## 2. 이벤트 수거 (AD-3)

SavePort 구현체(Outbound Adapter)가 `pullDomainEvents()`로 이벤트를 수거하여 Outbox에 저장한다. **같은 트랜잭션 내에서 원자적으로 수행한다.**

- MUST: SavePort 구현체가 `aggregate.pullDomainEvents()` → Outbox/이벤트 발행을 수행한다.
- MUST: 상태 변경(Entity → DB)과 이벤트 저장(Outbox)이 동일 트랜잭션으로 원자성을 보장해야 한다.
- MUST NOT: Application Service에서 `pullDomainEvents()`를 직접 호출한다.
- MUST NOT: Application Service에서 EventPublisher를 직접 호출한다.

```java
// ✅ GOOD — SavePort 구현체가 이벤트 수거 + Outbox 저장 (같은 TX)
@Component
public class UserPersistenceAdapter implements SaveUserPort {
    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher;  // Spring Modulith용

    @Override
    public void save(User user) {
        // 1. Entity → DB 저장 (UPDATE 시 Optimistic Lock 적용 — AD-7 참조)
        var record = toRecord(user);
        dsl.insertInto(USERS)
           .set(record)
           .onDuplicateKeyUpdate()
           .set(record)
           .execute();
        // UPDATE의 경우: .where(USERS.VERSION.eq(user.getVersion())) + affected==0 → OptimisticLockException

        // 2. Domain Event 수거 → Outbox(event_publication)에 저장 (같은 TX)
        user.pullDomainEvents().forEach(event ->
            eventPublisher.publishEvent(event)  // Spring Modulith: event_publication 테이블에 원자적 저장
        );
    }
}
```

---

## 3. 수동 매핑 (AD-4, AD-5)

### AD-4 명시적 매핑 강제 (Full Mapping 전략)

- MUST: Domain ↔ 기술 구조(jOOQ Record, Web DTO 등) 변환은 명시적 Mapper를 통해 수행한다.
- MUST: 모든 계층 경계에서 명시적 변환을 수행한다 (Hombergs Full Mapping 전략).
- MUST NOT: ModelMapper 등 **런타임 리플렉션 기반** 자동 매핑 라이브러리를 사용한다.
- MAY: **Adapter / Configuration 계층**에서 MapStruct(컴파일 타임 코드 생성)를 사용한다.
  - 조건: Mapper 인터페이스를 명시적으로 정의하고, 변환 규칙이 인터페이스에 선언되어 있어야 한다.
  - 금지: Domain / Application 계층에서 MapStruct 사용.
  - 금지: 암묵적 프로퍼티 이름 매칭에만 의존하는 Mapper (명시적 @Mapping 어노테이션 필수).
- MUST NOT: jOOQ Generated Record를 Domain 모듈에 노출한다.

> 근거: AD-4는 런타임 리플렉션 매핑(ModelMapper)의 타입 안전성 부재와 도메인 오염을 금지한다.
> MapStruct는 컴파일 타임 코드 생성으로 "명시적 매퍼" 원칙과 상충하지 않는다.
> build-recipe 라벨: `mapstruct` (adapter-input-api, adapter-output-persist 모듈에 필요 시 선언).

### AD-5 reconstitute() 사용

- MUST: DB에서 Aggregate를 로드할 때 반드시 `Aggregate.reconstitute()`를 호출한다.
- MUST NOT: DB 로드 시 `create()`를 호출하거나 `new`로 직접 생성한다.

```java
// Persistence Mapper — 수동 매핑
public class UserPersistenceMapper {

    // DB Record → Domain Model (reconstitute 사용)
    public User toDomain(UsersRecord record) {
        return User.reconstitute(
            new UserId(record.getId()),
            new UserName(record.getName()),
            UserStatus.valueOf(record.getStatus()),
            new OwnerId(record.getOwnerId()),
            record.getVersion()
        );
    }

    // Domain Model → DB Record
    public UsersRecord toRecord(User user) {
        var record = new UsersRecord();
        record.setId(user.id().value());
        record.setName(user.name().value());
        record.setStatus(user.status().name());
        record.setOwnerId(user.ownerId().value());
        record.setVersion(user.version());
        return record;
    }
}
```

전체 계층 매핑 흐름:

```
[HTTP Request]
    ↓ WebMapper.toCommand()
[Command]  (Application DTO — record, 원시 타입)
    ↓ Application Service: VO 생성 + Aggregate.create()
[Domain Model]
    ↓ PersistenceMapper.toRecord()
[jOOQ Record]
    ↓ DB 저장/조회
    ↓ PersistenceMapper.toDomain() → Aggregate.reconstitute()
[Domain Model]
    ↓ Application Service: Result DTO 생성
[Result]  (Application DTO — record)
    ↓ WebMapper.toResponse()
[HTTP Response]
```

---

## 4. Null Safety (AD-6)

- SHOULD: Adapter 및 Configuration 계층에서 JSpecify `@Nullable`/`@NonNull`을 적용한다.
- MUST NOT: Domain 또는 Application 계층에서 JSpecify 어노테이션을 사용한다.
- Domain/Application의 Null-Safety는 `Objects.requireNonNull`로 순수 Java 방식으로 보장한다 (D-14).

---

## 5. Optimistic Lock (AD-7)

- MUST: 모든 SavePort UPDATE 시 Aggregate의 `version` 필드를 WHERE 조건에 포함한다.
- MUST: 영향 행(affected rows)이 0이면 도메인 예외(`OptimisticLockException` 또는 커스텀 예외)를 발생시킨다.
- SHOULD: Optimistic Lock을 기본으로 선택한다.
- MUST: Pessimistic Lock 사용 시 근거를 ADR에 명시한다.

```java
// ✅ GOOD — jOOQ Optimistic Lock: WHERE version = ?
@Override
public void save(User user) {
    var record = mapper.toRecord(user);
    int affected = dsl.update(USERS)
        .set(USERS.NAME, record.getName())
        .set(USERS.STATUS, record.getStatus())
        .set(USERS.VERSION, user.version() + 1)   // version 증가
        .where(USERS.ID.eq(user.id().value()))
        .and(USERS.VERSION.eq(user.version()))     // Optimistic Lock 조건
        .execute();

    if (affected == 0) {
        throw new UserOptimisticLockException(user.id().value().toString());
    }

    // 이벤트 수거 → Outbox 저장
    user.pullDomainEvents().forEach(eventPublisher::publishEvent);
}
```

---

## 6. jOOQ 사용

- MUST: DB 접근은 jOOQ를 사용한다.
- MUST NOT: jOOQ(`DSLContext`, generated classes)를 Domain 또는 Application 계층에서 사용한다.
- jOOQ generated classes 위치: `build/generated-sources/jooq/`
- MUST: `DSLContext`는 Outbound Adapter에서만 주입받는다.

> 근거: ADR-0005

```java
// ✅ GOOD — jOOQ는 Adapter 계층에만
@Component
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {
    private final DSLContext dsl;                   // Adapter에서만 허용
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(UserId id) {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id.value()))
            .fetchOptional()
            .map(mapper::toDomain);                 // reconstitute() 호출 포함
    }
}

// QueryAdapter — DTO 직접 프로젝션 (Aggregate 비경유)
@Component
public class UserQueryAdapter implements UserQueryPort {
    private final DSLContext dsl;

    @Override
    public Optional<UserSummary> findSummaryById(String userId) {
        return dsl.select(USERS.ID, USERS.NAME, USERS.STATUS)
            .from(USERS)
            .where(USERS.ID.eq(UUID.fromString(userId)))
            .fetchOptional()
            .map(r -> new UserSummary(
                r.get(USERS.ID).toString(),
                r.get(USERS.NAME),
                r.get(USERS.STATUS)
            ));
    }
}
```

---

## 7. 예외 변환

기술 예외는 Adapter 경계에서 Domain/Application 예외로 변환한다. 기술 예외가 Core까지 전파되는 것을 금지한다.

```java
// ✅ GOOD — 기술 예외를 Adapter 경계에서 변환
@Override
public Optional<User> findById(UserId id) {
    try {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id.value()))
            .fetchOptional()
            .map(mapper::toDomain);
    } catch (DataAccessException e) {
        // 기술 예외 → Domain 예외로 변환
        throw new UserPersistenceException("Failed to load user: " + id.value(), e);
    }
}
```

| 계층 | 예외 유형 | 처리 |
|------|----------|------|
| Domain | 비즈니스 예외 (`DomainException`) | HTTP/기술 의존 절대 금지 |
| Adapter | 기술 예외 → 도메인/Application 예외로 변환 | 기술 예외가 Core까지 전파 금지 |
| Inbound Adapter | `@ControllerAdvice`로 RFC 9457 Problem Details 변환 | `ErrorCode`, `ErrorResponse`는 adapter-input에 위치 |

---

## 8. RFC 9457 예외 처리

- `@ControllerAdvice`(GlobalExceptionHandler)는 **`boilerplate-boot` 모듈 또는 `adapter-input-api` 모듈**에만 위치한다.
- `ErrorCode` enum, `ErrorResponse`(RFC 9457 Problem Details 호환)는 `adapter-input-*` 모듈에 위치한다.
- MUST NOT: `ErrorCode`, `ErrorResponse`를 Domain 또는 Application 계층에 위치시킨다.

```java
// adapter-input-api 모듈
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        problem.setType(URI.create("https://api.example.com/errors/user-not-found"));
        return problem;
    }
}
```

---

## fallback 지시문

---
> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호에 해당하는 `docs/decisions/` 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
