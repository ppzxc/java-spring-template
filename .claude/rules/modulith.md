---
description: Spring Modulith 하이브리드 전략, 모듈 구조, Outbox 패턴, EventTranslator
alwaysApply: true
---

# Spring Modulith Rules

Modulith 하이브리드 전략 규칙 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 하이브리드 전략 (ADR-0003)

**Gradle 멀티모듈(컴파일 타임) + Spring Modulith(런타임)**을 병행한다.

| 방어선 | 도구 | 역할 |
|--------|------|------|
| 1차 (컴파일) | Gradle 멀티모듈 | `dependencies {}` 선언이 없으면 import 자체 불가 |
| 2차 (테스트) | ArchUnit | 패키지 의존 방향 검증 |
| Outbox | Spring Modulith EventPublicationRegistry | `event_publication` 자동 트랜잭셔널 Outbox |
| 런타임 검증 | ApplicationModules.verify() | 허용되지 않은 모듈 간 참조 감지 |

- MUST: 신규 BC 추가 시 Gradle 멀티모듈 + `@Modulithic` 등록 모두 수행한다.
- MUST NOT: Common 유틸리티 모듈 (`*-common`, `*-util`)을 생성한다. 공유는 `shared-event` 모듈(이벤트만) 또는 test-support(픽스처만)로 제한한다.
- MUST: 멀티 모듈 구조를 사용한다. 단일 모듈 구조는 허용하지 않는다 (Part 3 §4.1).
- MUST: 컴파일 타임에 아키텍처 위반을 원천 차단한다. ArchUnit은 보조 수단일 뿐, 1차 방어선이 될 수 없다.

---

## 2. 모듈 등록

### settings.gradle.kts — 커스텀 module() 함수

```kotlin
val modules: MutableList<Module> = mutableListOf()
fun module(name: String, path: String) {
    modules.add(Module(name, "$rootDir/$path"))
}
data class Module(val name: String, val path: String)

// ── Apps ──────────────────────────────────────────────────────────────
module(name = ":boilerplate-boot",  path = "boilerplate/boilerplate-boot")

// ── Identity BC ──────────────────────────────────────────────────────
module(name = ":boilerplate-identity-domain",              path = "boilerplate/identity/boilerplate-identity-domain")
module(name = ":boilerplate-identity-application",         path = "boilerplate/identity/boilerplate-identity-application")
module(name = ":boilerplate-identity-configuration",       path = "boilerplate/identity/boilerplate-identity-configuration")
module(name = ":boilerplate-identity-adapter-input-api",   path = "boilerplate/identity/boilerplate-identity-adapter-input-api")
module(name = ":boilerplate-identity-adapter-output-persist", path = "boilerplate/identity/boilerplate-identity-adapter-output-persist")

modules.forEach {
    include(it.name)
    project(it.name).projectDir = file(it.path)
}
```

### @Modulithic 어노테이션 (boilerplate-boot)

```java
@Modulithic(
    systemName = "Boilerplate",
    sharedModules = "shared.event"   // Published Language 모듈
)
@SpringBootApplication
public class BoilerplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoilerplateApplication.class, args);
    }
}
```

---

## 3. 의존성 매트릭스

| 모듈 | 허용 의존 | build-recipe 라벨 |
|------|----------|-------------------|
| `{bc}-domain` | 없음 | `java` |
| `{bc}-application` | `{bc}-domain` | `java` |
| `{bc}-configuration` | `application` + `domain` + 전체 adapter | `java`, `spring` |
| `{bc}-adapter-input-api` | `{bc}-application` | `java`, `spring` |
| `{bc}-adapter-input-event` | `{bc}-application` | `java`, `spring` |
| `{bc}-adapter-output-persist` | `{bc}-application`, `{bc}-domain` | `java`, `spring`, `jooq` |
| `boilerplate-boot` | `{bc}-configuration` (전체) | `java`, `spring`, `boot` |

- MUST NOT: adapter 모듈이 다른 adapter 모듈을 직접 참조한다 (AD-2).
- MUST NOT: domain, application 모듈이 adapter 모듈을 참조한다.
- MUST: 의존 방향은 항상 바깥(boot) → 안쪽(domain). 역방향 금지.

---

## 4. build-recipe-plugin 라벨

linecorp build-recipe-plugin의 `configureByLabel()` 패턴. 모듈 `build.gradle.kts`에는 `label()`만 선언한다.

```kotlin
// boilerplate-identity-domain/build.gradle.kts
label("java")
label("coverage-gate")

// boilerplate-identity-adapter-output-persist/build.gradle.kts
label("java", "spring", "jooq")
dependencies {
    implementation(project(":boilerplate-identity-application"))
    implementation(project(":boilerplate-identity-domain"))
}

// boilerplate-boot/build.gradle.kts
label("java", "spring", "boot")
dependencies {
    implementation(project(":boilerplate-identity-configuration"))
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    implementation("org.springframework.modulith:spring-modulith-events-api")
    runtimeOnly(libs.org.postgresql.postgresql)
}
```

**라벨 정의 요약**

| 라벨 | 포함 내용 |
|------|----------|
| `java` | Java 25, ErrorProne, NullAway, Spotless, Checkstyle, ArchUnit, Fixture Monkey, JUnit5 |
| `spring` | spring-boot-starter, spring-boot-starter-validation, spring-boot-starter-json |
| `boot` | BootJar 활성화 |
| `jooq` | jOOQ Codegen + H2, spring-boot-starter-jooq |
| `coverage-gate` | JaCoCo 80% LINE 커버리지 게이트 |

---

## 5. Version Catalog (libs.versions.toml)

- MUST: 모든 의존성 버전은 `gradle/libs.versions.toml`에서 관리한다.
- MUST NOT: `build.gradle.kts`에 버전 리터럴을 직접 작성한다 (예: `"3.19.30"`).
- MUST: 신규 의존성 추가 시 `libs.versions.toml`에 버전 항목 먼저 등록 후 `libs.XXX` 참조.

**주요 버전 기준값**

| 라이브러리 | 버전 | 변수명 |
|-----------|------|--------|
| Spring Boot | 4.0.5 | `org-springframework-boot` |
| Spring Modulith | 2.0.1 | `spring-modulith` |
| Java | 25 | — |
| jOOQ | 3.19.30 | `org-jooq` |
| Testcontainers | 2.0.4 | `org-testcontainers` |
| ArchUnit | 1.4.1 | `com-tngtech-archunit` |
| Fixture Monkey | 1.1.19 | `com-navercorp-fixture-monkey` |

---

## 6. Shared Event 모듈 (Published Language)

BC 간 통신에는 Domain Event를 직접 사용하지 않고 Integration Event를 사용한다.

```
boilerplate-shared-event/
  src/main/java/.../shared/event/
    identity/
      UserRegisteredIntegrationEvent.java
      UserDeletedIntegrationEvent.java
    notification/
      NotificationSentIntegrationEvent.java
```

```java
// boilerplate-shared-event — 순수 Java, 기술 의존 없음
public record UserRegisteredIntegrationEvent(UUID userId, Instant occurredAt) {}
public record UserDeletedIntegrationEvent(UUID userId, Instant occurredAt) {}
```

- MUST: Integration Event는 순수 Java `record`로 정의한다. Spring/jOOQ 의존 금지.
- MUST: BC별 하위 패키지로 분리한다 (`shared/event/{bc명}/`).
- MUST NOT: Domain Event를 BC 간 통신에 직접 사용한다.
- MUST NOT: `@Externalized`를 Domain Event에 직접 붙인다 (Domain에 기술 어노테이션 금지 — D-2).

---

## 7. EventTranslator 패턴 (Domain → Integration)

Domain Event → Integration Event 변환은 **Configuration 모듈**에서 수행한다.

```java
// boilerplate-identity-configuration
@Component
class IdentityEventTranslator {
    private final ApplicationEventPublisher publisher;

    IdentityEventTranslator(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener
    void on(UserRegisteredEvent event) {
        publisher.publishEvent(new UserRegisteredIntegrationEvent(
            event.aggregateId(),
            event.occurredAt()
        ));
    }
}
```

- MUST: EventTranslator는 Configuration 모듈에 위치한다.
- MUST: `@TransactionalEventListener`로 Domain Event를 수신하고 Integration Event로 재발행한다.
- MUST NOT: Application Service에서 `ApplicationEventPublisher`를 직접 호출한다 (A-6 — cqrs.md 참조).
- MUST NOT: EventTranslator를 Application 모듈에 위치시킨다.

> `@TransactionalEventListener`는 Configuration 모듈에서 사용하므로 A-4(@Transactional 금지)의 위반이 아니다.

---

## 8. BC 간 / BC 내부 이벤트 통신

### BC 간 통신 (Integration Event 경유)

```java
// {bc}-adapter-input-event 모듈
@Component
class UserEventHandler {
    private final SendWelcomeNotificationUseCase useCase;

    UserEventHandler(SendWelcomeNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @ApplicationModuleListener
    void on(UserRegisteredIntegrationEvent event) {
        useCase.execute(new SendWelcomeNotificationCommand(
            event.userId().toString(),
            event.occurredAt().toString()
        ));
    }
}
```

### BC 내부 Aggregate 간 통신 (Domain Event 직접)

```java
// {bc}-adapter-input-event — 같은 BC 내 Aggregate 간
@Component
class TeamEventHandler {
    private final AddTeamMemberUseCase useCase;

    @ApplicationModuleListener
    void on(UserJoinedTeamEvent event) {
        useCase.execute(new AddTeamMemberCommand(
            event.teamId().toString(),
            event.aggregateId().toString()
        ));
    }
}
```

| 구분 | BC 간 | BC 내부 |
|------|:-----:|:-------:|
| 이벤트 타입 | Integration Event (shared-event) | Domain Event (같은 BC) |
| EventTranslator | 필요 | 불필요 |
| 모듈 | `{bc}-adapter-input-event` | `{bc}-adapter-input-event` |

- MUST: BC 간 통신은 `@ApplicationModuleListener`로 Integration Event를 수신한다.
- MUST: 이벤트 수신 핸들러는 `adapter-input-event` 모듈에 위치한다.
- MUST: 핸들러는 UseCase를 통해 비즈니스 처리한다. 핸들러에서 직접 Port를 호출하지 않는다.

---

## 9. Event Publication Registry (Outbox)

Spring Modulith가 `event_publication` 테이블을 자동으로 트랜잭셔널 Outbox로 사용한다.

```
event_publication
├── id                 (PK)
├── listener_id        (처리 대상 리스너)
├── event_type         (이벤트 클래스명)
├── serialized_event   (JSON)
├── publication_date   (발행 시각)
└── completion_date    (처리 완료 시각 — NULL = 미처리)
```

**application.yml 설정**

```yaml
spring:
  modulith:
    events:
      jdbc-schema-initialization:
        enabled: true           # event_publication 테이블 자동 생성
      completion-mode: delete   # 처리 완료 시 즉시 삭제
    republish-outstanding-events-on-restart: true  # 재시작 시 미처리 이벤트 재발행
```

**테스트 환경 (`application-test.yml`)**

```yaml
spring:
  modulith:
    events:
      jdbc-schema-initialization.enabled: true
      completion-mode: delete
    republish-outstanding-events-on-restart: false  # 테스트에서 재발행 금지
```

- MUST: `jdbc-schema-initialization.enabled: true`를 설정하여 테이블 자동 생성을 활성화한다.
- MUST: SavePort의 `pullDomainEvents()` + `publishEvent()` 호출과 DB 저장은 **동일 트랜잭션** 내에서 수행한다 (AD-3 — adapter.md 참조).

---

## 10. BC 확장 패턴

신규 BC 추가 시 반드시 다음 모듈을 생성하고 settings.gradle.kts에 등록한다.

```
boilerplate/{bc}/
  boilerplate-{bc}-domain/
  boilerplate-{bc}-application/
  boilerplate-{bc}-configuration/
  boilerplate-{bc}-adapter-input-api/        (필요 시)
  boilerplate-{bc}-adapter-input-event/      (이벤트 수신 필요 시)
  boilerplate-{bc}-adapter-output-persist/
```

**settings.gradle.kts 등록 예시 (notification BC 추가)**

```kotlin
module(name = ":boilerplate-notification-domain",      path = "boilerplate/notification/boilerplate-notification-domain")
module(name = ":boilerplate-notification-application", path = "boilerplate/notification/boilerplate-notification-application")
module(name = ":boilerplate-notification-configuration", path = "boilerplate/notification/boilerplate-notification-configuration")
module(name = ":boilerplate-notification-adapter-input-event", path = "boilerplate/notification/boilerplate-notification-adapter-input-event")
module(name = ":boilerplate-notification-adapter-output-persist", path = "boilerplate/notification/boilerplate-notification-adapter-output-persist")
```

- MUST: 신규 BC의 모든 모듈은 기존 `boilerplate-{bc}-{layer}` 네이밍 패턴을 따른다.
- MUST NOT: 특정 BC에만 사용되는 공유 유틸을 별도 모듈(`*-common`)로 분리한다.

---

## 11. 마이크로서비스 전환 경로

모놀리식 Modulith에서 마이크로서비스로 전환하는 판단 기준과 절차.

### 전환 판단 기준

마이크로서비스 분리는 **기술적 이유가 아닌 비즈니스/운영 필요**에 의해 결정한다.

| 신호 | 설명 |
|------|------|
| **팀 자율성** | 팀 간 배포 병목이 심각하여 독립 배포가 필요한 경우 |
| **기술 스택 분리** | 특정 BC가 다른 언어/런타임이 필요한 경우 |
| **독립 스케일링** | 특정 BC만 급격한 부하 증가가 발생하는 경우 |
| **장애 격리** | 한 BC 장애가 전체 서비스에 영향을 미치는 경우 |

- MUST NOT: 성능 최적화, 코드 정리, 기술 트렌드를 이유로 분리한다.
- SHOULD: Modulith에서 Aggregate 경계가 잘 정의되어 있지 않으면 분리 전에 경계를 먼저 정리한다.

### 전환 전 사전 조건

- [ ] BC 간 통신이 모두 Integration Event를 통한다 (직접 메서드 호출 없음)
- [ ] ApplicationModules.verify() 통과
- [ ] `@ApplicationModuleTest(STANDALONE)` 단독 기동 가능
- [ ] 해당 BC의 DB 스키마가 명확히 분리되어 있음

### 전환 절차

```
[1단계] 모듈 경계 검증
  → ApplicationModules.verify() + STANDALONE 테스트 통과 확인

[2단계] 네트워크 이벤트로 교체
  → Spring Modulith in-process 이벤트 → 외부 메시지 브로커(Kafka/RabbitMQ)로 교체
  → EventPublisher 구현체만 교체 (Adapter 계층 변경)

[3단계] 독립 배포 단위 분리
  → Gradle 멀티모듈에서 별도 프로젝트로 추출
  → 독립 Spring Boot 애플리케이션으로 구성

[4단계] 서비스 통신 방식 확정
  → 동기: REST API (OpenAPI 계약 우선)
  → 비동기: 메시지 브로커 (CloudEvents 표준 준수 — cqrs.md §12 참조)
```

- MUST: 전환 결정은 ADR로 문서화한다.

---

## fallback 지시문

---
> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> `docs/decisions/ADR-0003-*.md` 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
