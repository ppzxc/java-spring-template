---
paths: "**/domain/**"
---

# Domain Layer Rules

도메인 계층 규칙 — `**/domain/**` 경로에 자동 적용.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 순수성 제약

### D-1 외부 의존성 제로

- MUST NOT: `build.gradle.kts`의 `dependencies {}` 블록에 외부 라이브러리를 추가한다.
- MUST: `dependencies {}` 블록이 완전히 비어 있어야 한다. 예외 없음.
- 금지 예시: Spring, jOOQ, Jackson, Lombok, SLF4J, Log4j, jakarta.validation, 어떠한 프레임워크/라이브러리도 금지.

> 근거: ADR-0001

### D-2 프레임워크 어노테이션 금지

- MUST NOT: 도메인 클래스에 프레임워크 어노테이션을 사용한다.
- 금지 어노테이션: `@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@Component`, `@Service`, `@Repository`, `@Valid`, `@NotNull`, `@JsonProperty` 등 일체 금지.

> 근거: ADR-0014

### D-3 로깅 프레임워크 금지

- MUST NOT: 도메인 클래스에서 로깅 프레임워크(SLF4J, Log4j, java.util.logging)를 사용한다.
- MUST: 상태 변화는 반환값, 예외, 도메인 이벤트로만 표현한다.

> 근거: ADR-0015

### D-4 시스템 시계 금지

- MUST NOT: 도메인 내부에서 `Instant.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()`, `Clock`을 직접 호출한다.
- MUST: 시간 정보(`Instant`)는 Application 계층이 파라미터로 전달한다.

---

## 2. Rich Domain Model (D-5)

비즈니스 로직은 Entity/VO의 행위 메서드 내부에 위치한다. Application Service에 if/else 비즈니스 판단이 있으면 안티패턴이다.

- MUST: Entity의 상태 변경은 행위 메서드를 통해서만 수행한다.
- MUST NOT: public Setter를 노출한다.
- MUST NOT: Application Service에 비즈니스 정책 판단(if/else 비즈니스 분기)을 둔다.

```java
// ❌ BAD — Anemic Domain Model: Entity는 데이터 구조, 로직은 Service에
public class User {
    private UserStatus status;
    public void setStatus(UserStatus status) { this.status = status; }
}

public class SuspendUserService {
    public void execute(SuspendUserCommand cmd) {
        var user = loadPort.findById(new UserId(cmd.userId())).orElseThrow();
        if (user.getStatus() != UserStatus.ACTIVE) {   // ← 비즈니스 판단이 Service에!
            throw new UserNotActiveException();
        }
        user.setStatus(UserStatus.SUSPENDED);           // ← Setter로 직접 변경
        savePort.save(user);
    }
}

// ✅ GOOD — Rich Domain Model: 비즈니스 로직이 Entity 내부에 캡슐화
public final class User {
    private UserStatus status;

    public void suspend(String reason, Instant occurredAt) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (this.status != UserStatus.ACTIVE) {
            throw new UserNotActiveException(this.id.value().toString());
        }
        this.status = UserStatus.SUSPENDED;
        registerEvent(new UserSuspendedEvent(
            UUIDv7.generate(), "UserSuspendedEvent", this.id.value(), occurredAt, this.version, reason
        ));
    }
}

// Application Service는 위임만
public class SuspendUserService {
    public void execute(SuspendUserCommand cmd) {
        var user = loadPort.findById(new UserId(cmd.userId())).orElseThrow();
        user.suspend(cmd.reason(), clock.instant());    // ← Entity에 위임
        savePort.save(user);
    }
}
```

---

## 3. Value Object (D-6, D-7)

### D-6 Value Object는 record 강제

- MUST: 모든 Value Object는 Java `record`로 구현한다. 예외 없음.
- MUST: Compact Constructor에서 자기 검증(Self-Validation)을 수행한다.
- MUST: 컬렉션 필드는 Compact Constructor에서 `List.copyOf()` / `Set.copyOf()`로 방어적 복사한다.
- MUST NOT: VO에 Setter나 상태 변경 메서드를 둔다.

```java
public record Email(String value) {
    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}

public record UserName(String value) {
    public UserName {
        Objects.requireNonNull(value, "UserName must not be null");
        if (value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException("Invalid userName: " + value);
        }
    }
}
```

### D-7 Primitive Obsession 금지

- MUST NOT: 도메인 의미를 가지는 원시 타입 값(`String email`, `Long userId`, `String name`)을 VO로 포장하지 않고 그대로 전달한다.
- MUST: 도메인 의미 있는 모든 값을 `record` VO로 포장한다.

```java
// ❌ BAD — 원시 타입: email과 name을 바꿔 넣어도 컴파일 에러 없음
public static User create(String name, String email) { ... }

// ✅ GOOD — record VO: 타입이 다르므로 컴파일 에러로 버그 방지
public static User create(UserName name, Email email) { ... }
```

### Entity vs VO 결정 기준

| 기준 | Entity | Value Object |
|---|---|---|
| 식별 방식 | ID로 구분 | 모든 속성 값으로 구분 |
| 가변성 | 상태가 변할 수 있음 (행위 메서드로) | 불변 (`record`, 변경 시 새 인스턴스) |
| 동등성 | ID 동일 시 동일 객체 (`equals/hashCode`는 ID 기반) | 모든 속성 동일 시 동일 객체 |
| 생명주기 | 생성/변경/소멸 추적 | 생명주기 없음 |
| 본 프로젝트 표현 | `final class` + `private` 생성자 | `record` + Compact Constructor |
| 예시 | `User`, `Order`, `Account` | `Email`, `UserName`, `UserId` |

---

## 4. Aggregate 생명주기 (D-8, D-9)

### D-8 create() / reconstitute() 분리

모든 Aggregate Root는 두 팩토리 메서드를 분리한다. **public 생성자 금지.**

| 메서드 | 용도 | 이벤트 발행 | 호출자 |
|--------|------|:---:|--------|
| `create()` | 신규 생성 | O | Application Service |
| `reconstitute()` | DB 복원 | X | Adapter의 PersistenceMapper |

- MUST: Aggregate Root의 생성자는 `private`으로 선언한다.
- MUST: 신규 생성은 `create()` 정적 팩토리 메서드만 사용한다. 이벤트를 발행한다.
- MUST: DB 복원은 `reconstitute()` 정적 팩토리 메서드만 사용한다. 이벤트를 발행하지 않는다.
- MUST NOT: `new` 키워드로 외부에서 Aggregate Root를 직접 생성한다.
- MUST NOT: `reconstitute()`에서 이벤트를 발행한다.
- MUST: Aggregate Root 클래스에 `final` 키워드를 붙인다.
- MUST: 두 Entity 인스턴스의 ID가 동일하면 동일 객체로 간주한다 (`equals/hashCode`는 ID 기반).

```java
public final class User {
    private final UserId id;
    private UserName name;
    private UserStatus status;
    private final OwnerId ownerId;
    private final long version;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // private 생성자 — 팩토리 메서드를 통해서만 생성
    private User(UserId id, UserName name, UserStatus status,
                 OwnerId ownerId, long version) {
        this.id = Objects.requireNonNull(id, "UserId must not be null");
        this.name = Objects.requireNonNull(name, "UserName must not be null");
        this.status = Objects.requireNonNull(status, "UserStatus must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "OwnerId must not be null");
        this.version = version;
    }

    // 신규 생성 — 이벤트 발행
    public static User create(UserName name, OwnerId ownerId, Instant now) {
        var user = new User(UserId.generate(), name, UserStatus.ACTIVE, ownerId, 0L);
        user.registerEvent(new UserRegisteredEvent(
            UUIDv7.generate(), "UserRegisteredEvent", user.id.value(), now, 0L, name.value()
        ));
        return user;
    }

    // DB 복원 — 이벤트 미발행
    public static User reconstitute(UserId id, UserName name,
                                    UserStatus status, OwnerId ownerId,
                                    long version) {
        return new User(id, name, status, ownerId, version);
    }

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    public UserId id() { return this.id; }
    public UserName name() { return this.name; }
    public UserStatus status() { return this.status; }
    public OwnerId ownerId() { return this.ownerId; }
    public long version() { return this.version; }
}
```

### D-9 Aggregate 간 객체 참조 금지

- MUST NOT: 다른 Aggregate 객체를 직접 필드로 보유(참조)한다.
- MUST: 다른 Aggregate를 참조할 때 ID(VO)만 사용한다.
- MUST NOT: 하나의 UseCase에서 2개 이상의 Aggregate를 동일 트랜잭션으로 변경한다.

```java
// ❌ BAD — 다른 Aggregate 객체를 직접 참조
public final class Order {
    private User user;   // Aggregate 간 객체 참조 금지
}

// ✅ GOOD — ID만 참조
public final class Order {
    private final UserId userId;   // ID 참조만 허용
}
```

### Aggregate Root 4대 역할

| 역할 | 책임 | 본 프로젝트 강제 수단 |
|---|---|---|
| 유일 진입점 | 외부는 Root를 통해서만 내부 Entity 접근 | 내부 Entity의 `create()`/`reconstitute()`를 package-private으로 선언 |
| 일관성 경계 | Aggregate 내부 불변식 보장 | Root 행위 메서드 내부에서만 상태 변경 |
| 도메인 이벤트 발행 | 상태 변경 시 `registerEvent()` | `pullDomainEvents()` 수거 패턴 (AD-3) |
| 외부 참조 ID 전용 | 다른 Aggregate는 ID로만 참조 | `Set<RoleId>` 패턴 (D-9) |

### Aggregate 내부 Entity 접근 제어

Aggregate Root가 내부 Entity(예: Credential, OrderItem)를 포함하는 경우, 외부에서 내부 Entity를 직접 생성하는 것을 차단한다.

- MUST: 내부 Entity의 `create()` / `reconstitute()` 팩토리 메서드는 **package-private** 접근 수준으로 선언한다.
- MUST: 내부 Entity의 생성자는 `private`으로 선언한다.
- MUST: Aggregate Root만 내부 Entity를 생성·복원할 수 있다.
- MUST NOT: 외부 패키지에서 내부 Entity를 직접 생성한다.

```java
// ✅ GOOD — 내부 Entity: package-private 팩토리
final class Credential {
    private final CredentialId id;
    private final HashedPassword password;

    private Credential(CredentialId id, HashedPassword password) {
        this.id = Objects.requireNonNull(id);
        this.password = Objects.requireNonNull(password);
    }

    // package-private — Aggregate Root(User)만 호출 가능
    static Credential create(HashedPassword password, Instant now) {
        return new Credential(CredentialId.generate(), password);
    }

    // package-private — PersistenceMapper가 같은 패키지에서 호출
    static Credential reconstitute(CredentialId id, HashedPassword password) {
        return new Credential(id, password);
    }
}

// Aggregate Root가 내부 Entity 생성을 관장
public final class User {
    private Credential credential;

    public void changePassword(HashedPassword newPassword, Instant now) {
        this.credential = Credential.create(newPassword, now);
        registerEvent(/* ... */);
    }
}
```

---

## 5. Domain 경계

> **D-12 (금지 접미사)** — naming.md §금지 접미사 참조. Handler/Processor/Manager/Helper/Util/VO/Entity 접미사 금지.

### D-10 Domain에 Repository/Port 금지

- MUST NOT: Domain 계층에 Repository 인터페이스 또는 Output Port 인터페이스를 정의한다.
- MUST: Output Port(LoadPort, SavePort, QueryPort)는 Application 계층에만 위치한다.

> 근거: ADR-0006

> **Evans 원전과의 차이**: Evans(2003)와 Vernon(2013) 원전에서는 Repository 인터페이스가 Domain에 위치한다. 본 프로젝트는 헥사고날 아키텍처의 포트 분리 원칙(DIP)을 우선하는 의도된 선택이다. 이로써 Domain 모듈의 `dependencies {}`가 완전히 비어 있음을 보장하고, 영속성 기술 교체 시 Domain 코드 변경이 제로가 된다.

### D-11 Domain Service → Port 호출 금지

- MUST NOT: Domain Service가 Port, Repository를 주입받거나 호출한다.
- MUST NOT: 읽기 전용 Query Port도 예외 없이 Domain Service에 주입 금지.
- MUST: 필요한 데이터는 Application Service가 파라미터로 전달한다.
- MUST: Domain Service는 Stateless로 구현한다. 필드에 상태를 보유하지 않는다.
- MUST: 순수 Java로 구현한다. `{Subject}DomainService`로 네이밍한다.
- MUST NOT: 프레임워크 어노테이션, 로깅, 시스템 시계를 사용한다.

> 근거: ADR-0007

---

## 6. Sealed 그룹화 (D-13)

### Domain Event — sealed interface

- MUST: Domain Event는 Aggregate별 `sealed interface`로 그룹화한다.
- MUST: 각 이벤트 구현체는 불변 `record`로 구현한다.
- MUST: 과거형으로 네이밍한다 (`UserRegisteredEvent`, `UserSuspendedEvent`).
- MUST NOT: Application Service나 Adapter에서 Domain Event를 직접 생성한다.

```java
// DomainEvent 인터페이스 — 각 BC의 domain 모듈에 독립 정의 (BC 간 공유 금지)
public interface DomainEvent {
    UUID eventId();
    String eventType();
    UUID aggregateId();
    Instant occurredAt();
    long aggregateVersion();
}

// Aggregate별 sealed interface
public sealed interface UserEvent extends DomainEvent
    permits UserRegisteredEvent, UserSuspendedEvent, UserDeletedEvent {}

// 각 이벤트 — 불변 record, 5필드 필수
public record UserRegisteredEvent(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    Instant occurredAt,
    long aggregateVersion,
    String userName          // 이벤트 고유 페이로드
) implements UserEvent {}

public record UserSuspendedEvent(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    Instant occurredAt,
    long aggregateVersion,
    String reason
) implements UserEvent {}
```

sealed interface + switch 패턴 매칭으로 이벤트 핸들링 누락을 컴파일 타임에 방지한다:

```java
public void handle(UserEvent event) {
    switch (event) {
        case UserRegisteredEvent e -> onCreated(e);
        case UserSuspendedEvent e -> onSuspended(e);
        case UserDeletedEvent e -> onDeleted(e);
        // 새 이벤트를 permits에 추가하면 여기서 컴파일 에러 → 핸들링 누락 원천 차단
    }
}
```

### Domain Exception — sealed class

- MUST: Domain Exception은 `sealed class`로 그룹화한다.
- MUST NOT: Domain Exception에 HTTP 상태 코드, Spring 어노테이션 등 기술 의존을 포함한다.

```java
// 베이스 예외 — abstract sealed class
public abstract sealed class UserException extends RuntimeException
    permits UserNotFoundException, UserAlreadyExistsException, UserNotActiveException {
    protected UserException(String message) { super(message); }
}

public final class UserNotFoundException extends UserException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}

public final class UserAlreadyExistsException extends UserException {
    public UserAlreadyExistsException(String name) {
        super("User already exists: " + name);
    }
}

public final class UserNotActiveException extends UserException {
    public UserNotActiveException(String userId) {
        super("User is not active: " + userId);
    }
}
```

---

## 7. Null Safety (D-14)

- MUST: Entity/Domain Service의 모든 행위 메서드 파라미터에 `Objects.requireNonNull`로 null을 거부한다.
- MUST: JSpecify 없이 순수 Java로 Null-Safety를 보장한다.
- MUST NOT: Domain/Application 계층에 JSpecify `@Nullable`/`@NonNull`을 사용한다 (Adapter/Configuration에만 허용).

```java
public void suspend(String reason, Instant occurredAt) {
    Objects.requireNonNull(reason, "reason must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    // 비즈니스 로직...
}
```

---

## 8. Aggregate 설계 원칙

### 크기 원칙 (Vernon — "가능한 작게")

- MUST: Aggregate를 가능한 작게 유지한다. 락 경합 최소화, 이벤트 기반 확장성 확보.
- 판단 기준: "항상 함께 일관되어야 하는가?" — 그렇지 않으면 별도 Aggregate로 분리하고 이벤트로 연결.

| IF | THEN |
|----|------|
| 반드시 동시에 일관되어야 한다 | **같은 Aggregate** |
| Eventually Consistent로 충분하다 | **분리 + 이벤트** |
| 불분명하다 | **분리한다.** 합치기가 쪼개기보다 쉽다 |

### 강한 일관성 경계

- MUST: Aggregate Root만 public 행위 메서드를 노출한다. 내부 Entity는 Root를 통해서만 변경한다.
- MUST: 1 트랜잭션 = 1 Aggregate. 예외 없음.

### Aggregate 간 Eventually Consistent

여러 Aggregate 변경이 필요하면 이벤트 기반 분리:

```
[1st TX] Aggregate A 변경 → Domain Event 발행 → Outbox에 원자적 저장
[2nd TX] Event 핸들러(adapter-input-event) → Command → UseCase → Aggregate B 변경 (별도 TX)
```

---

## 9. Domain Event 설계

### DomainEvent 인터페이스

- MUST: 각 BC의 domain 모듈에 DomainEvent 인터페이스를 독립 정의한다.
- MUST NOT: DomainEvent 인터페이스를 BC 간 공유 모듈로 추출한다.

### 5-field 표준

모든 Domain Event에 다음 5필드를 포함한다. 예외 없음.

| 필드 | 역할 | 타입 |
|------|------|------|
| `eventId` | 멱등성 키 — 컨슈머 중복 감지 | UUID |
| `eventType` | 역직렬화 판별자 | String |
| `aggregateId` | 파티셔닝 키 — 순서 보장 | UUID |
| `occurredAt` | 발생 시각 (Application이 전달) | Instant |
| `aggregateVersion` | Aggregate 버전 — 순서 검증 | long |

- MUST NOT: Domain 내부에서 `Instant.now()`를 직접 호출하여 `occurredAt`을 채운다.
- MUST: `occurredAt`은 Application 계층에서 `Clock`을 통해 얻은 `Instant`를 파라미터로 전달받는다.

### pullDomainEvents() 패턴

```java
private final List<DomainEvent> domainEvents = new ArrayList<>();

private void registerEvent(DomainEvent event) {
    domainEvents.add(event);
}

public List<DomainEvent> pullDomainEvents() {
    var events = List.copyOf(domainEvents);
    domainEvents.clear();   // 한 번만 수거 — 호출 시 내부 목록 비움
    return events;
}
```

- MUST: Application Service는 `savePort.save(aggregate)`만 호출한다. 이벤트 수거·발행은 Adapter(SavePort 구현체)가 수행한다.
- MUST NOT: Application Service에서 `pullDomainEvents()`를 직접 호출한다.
- MUST NOT: Application Service에서 EventPublisher를 직접 호출한다.

---

### 이벤트 발행 판단 기준

모든 Aggregate 상태 변경이 이벤트를 발행해야 하는 것은 아니다.

| 상황 | 발행 여부 | 근거 |
|------|---------|------|
| 다른 BC가 이 변경에 반응해야 함 | **발행** | BC 간 Eventually Consistent |
| 같은 BC 내 다른 Aggregate가 반응해야 함 | **발행** | 1 TX = 1 Aggregate 원칙 |
| 감사(Audit) 로그가 필요함 | **발행** | 이벤트 없이 감사 추적 불가 |
| 단순 내부 상태 변경 (다른 소비자 없음) | **선택** | 불필요한 복잡성 회피 |

- MUST NOT: 컨슈머가 없는 이벤트를 "언젠가 쓸 수도 있으니" 발행한다.
- SHOULD: 처음에는 소비자가 생길 때 이벤트를 추가한다 (YAGNI).

### Thin Event vs Fat Event

| 패턴 | 페이로드 | 장점 | 단점 |
|------|---------|------|------|
| **Thin Event** | ID + 발생 시각만 포함 | 스키마 안정, 단순 | 컨슈머가 추가 조회 필요 |
| **Fat Event** | 변경된 전체 상태 포함 | 컨슈머 추가 조회 불필요 | 페이로드 비대, 스키마 변경 잦음 |

**본 프로젝트 기본 선택: Thin Event**

```java
// ✅ GOOD — Thin Event: 컨슈머가 추가 조회 가능한 경우
public record UserRegisteredEvent(
    UUID eventId, String eventType, UUID aggregateId,
    Instant occurredAt, long aggregateVersion
    // 추가 페이로드 없음
) implements UserEvent {}

// ✅ ALSO GOOD — Fat Event: 조회 불가하거나 이벤트 소싱인 경우
public record UserRegisteredEvent(
    UUID eventId, String eventType, UUID aggregateId,
    Instant occurredAt, long aggregateVersion,
    String userName, String email    // 생성 시점 스냅샷
) implements UserEvent {}
```

- SHOULD: 기본적으로 Thin Event를 사용한다.
- MAY: 이벤트 소싱 패턴 또는 컨슈머가 추가 조회를 할 수 없는 경우 Fat Event를 선택한다.
- MUST NOT: PII(개인 식별 정보)를 Fat Event 페이로드에 포함한다.

---

### append-only Aggregate 변형

소비 전용 BC(예: Audit BC)에서는 Aggregate가 이벤트를 발행하지 않는 변형이 허용된다.

- MAY: 소비 전용 BC에서는 `create()`에서 이벤트를 발행하지 않을 수 있다.
- MAY: `pullDomainEvents()` 메서드와 `domainEvents` 리스트를 생략할 수 있다.
- MUST: 이 경우에도 `create()` / `reconstitute()` 팩토리, `private` 생성자, `Objects.requireNonNull`은 동일 적용한다.

```java
// append-only Aggregate 예시 — AuditLog (이벤트 미발행)
public final class AuditLog {
    private final AuditLogId id;
    private final String action;
    private final Instant occurredAt;

    private AuditLog(AuditLogId id, String action, Instant occurredAt) {
        this.id = Objects.requireNonNull(id);
        this.action = Objects.requireNonNull(action);
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public static AuditLog create(String action, Instant now) {
        return new AuditLog(AuditLogId.generate(), action, now);
        // 이벤트 발행 없음 — 이 Aggregate 자체가 이벤트의 소비 결과
    }

    public static AuditLog reconstitute(AuditLogId id, String action, Instant occurredAt) {
        return new AuditLog(id, action, occurredAt);
    }
}
```

---

## 10. Anti-Patterns

| 안티패턴 | 증상 | 해결 |
|---------|------|------|
| **Anemic Domain Model** | Entity가 Getter/Setter만 가짐. 비즈니스 로직이 Application Service에 있음. | 비즈니스 로직을 Entity/VO의 행위 메서드로 이동. [D-5] |
| **God Aggregate** | 수십 개 Entity, 수백 개 필드. 모든 변경에 전체 Aggregate 로드. | 불변식 재분석 후 분리. "항상 함께 일관되어야 하는가?" [D-9] |
| **Direct Aggregate Reference** | 다른 Aggregate 객체를 필드로 보유. | ID(VO)만 참조. 객체 참조 금지. [D-9] |
| **Direct EventPublisher Call** | Application Service에서 EventPublisher 직접 호출. | SavePort 구현체(Adapter)가 pullDomainEvents() 후 발행. [AD-3] |
| **Leaky Abstraction** | Domain Entity에 `@Entity`, `@Table` 등 기술 어노테이션 침투. | 순수 Java만. 기술 매핑은 Adapter Mapper에서 수동 수행. [D-1, D-2] |
| **Primitive Obsession** | 도메인 의미가 있는 값을 `String`, `Long` 등 원시 타입으로 직접 사용. | record VO로 래핑. `UserId(UUID)`, `Email(String)` 등. [D-7] |
| **Service Orchestration Overload** | Application Service에 if/else 비즈니스 판단이 집중. Domain이 빈 껍데기. | 비즈니스 판단을 Domain Entity/VO의 행위 메서드로 위임. [A-5] |
| **Cross-Aggregate Transaction** | 하나의 UseCase에서 복수 Aggregate를 동일 TX로 변경. | 이벤트 기반으로 분리. 1 TX = 1 Aggregate. [A-9] |
| **Event Amnesia** | SavePort 구현체에서 `pullDomainEvents()` 호출 누락. 이벤트 소실. | `save()` 내부에서 반드시 수거 후 `ApplicationEventPublisher.publishEvent()`. [AD-3] |
| **Shotgun Parsing** | 입력 검증이 Controller, Service, Domain에 분산. 어디서 실패할지 불명확. | 검증 책임 분리: Command self-validation → VO Compact Constructor → Entity 불변식 순서 엄수. |

---

## fallback 지시문

---
> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호에 해당하는 `docs/decisions/` 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
