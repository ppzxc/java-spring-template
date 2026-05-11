---
description: OpenTelemetry, 구조화 로깅, ScopedValue 컨텍스트 전파 (A-11), HealthIndicator
alwaysApply: true
---

# Observability Rules

관측성 규칙 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 세 기둥 (Three Pillars)

| 신호 | 정의 | 도구 |
|------|------|------|
| **Logs** | 무슨 일이 일어났는가? | ECS JSON (Logback), traceId 자동 주입 |
| **Traces** | 요청이 어떤 경로로 흘렀는가? | W3C Trace Context, OTLP export |
| **Metrics** | 얼마나, 얼마나 자주? | Micrometer → OTLP export → Grafana |

세 신호는 traceId로 상관(correlate)되어야 비로소 관측성이 달성된다.

---

## 2. OpenTelemetry 설정

### 의존성

```kotlin
// boilerplate-boot/build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

- MUST: `spring-boot-starter-opentelemetry` 사용 (Spring 공식 스타터).
- MUST NOT: 3rd-party OTel 스타터 또는 Java Agent를 사용한다.
- MUST: Spring Boot 4.0.1 이상 사용 (4.0.0은 로그 export 버그).

### application.yml

```yaml
spring:
  application:
    name: boilerplate

management:
  tracing:
    sampling:
      probability: 1.0    # 개발: 100%, 운영: 0.1 권장
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
    tracing:
      export:
        url: http://localhost:4318/v1/traces
    logging:
      export:
        url: http://localhost:4318/v1/logs

logging:
  structured:
    format:
      console: ecs
```

### 개발 환경 (LGTM Stack)

```yaml
# docker-compose.yml
services:
  lgtm:
    image: grafana/otel-lgtm:latest
    ports:
      - "3000:3000"    # Grafana UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP
```

Spring Boot Docker Compose 자동 지원: `spring-boot-docker-compose` 의존성 추가 시 LGTM 자동 감지 + OTLP 엔드포인트 자동 설정.

---

## 3. 구조화 로깅 (ECS JSON)

```json
{
  "@timestamp": "2026-04-12T10:30:45.123Z",
  "log": {"level": "INFO", "logger": "i.g.p.RegisterUserService"},
  "service": {"name": "boilerplate"},
  "message": "User registered",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b7ad6b7169203331",
  "ecs": {"version": "8.11"}
}
```

- MUST: `traceId`, `spanId`는 OpenTelemetry가 MDC에 자동 주입. 별도 코드 불필요.
- MUST NOT: 로그에 PII(개인 식별 정보), 비밀번호, API 키를 기록한다.
- MUST NOT: Domain 객체 전체(`toString()`)를 로그에 기록한다. ID 값만 기록한다.

```java
// ❌ BAD — toString()에 email, password 포함 가능
log.info("User registered: {}", user);

// ✅ GOOD — ID만 기록
log.info("User registered: id={}", user.id().value());
```

---

## 4. 관측성 코드 위치 규칙

| 관측성 요소 | 허용 위치 | 금지 위치 |
|------------|---------|----------|
| `@Observed` / ObservationRegistry | Configuration | Domain, Application |
| 구조화 로깅 코드 (`log.info`) | Adapter, Configuration | Domain |
| HealthIndicator | Adapter, Configuration | Domain, Application |
| ScopedValue 바인딩 | Adapter (In) Filter | Domain, Application |
| MDC 읽기 | 자동 (OTel) | — |

- MUST NOT: Domain 모듈에서 로깅/메트릭 코드를 사용한다 (D-3, D-1).
- MUST NOT: Application 모듈에서 `@Observed`, Micrometer API를 사용한다 (A-1).

### Configuration에서 Observation 래핑 예시

```java
@Bean
public RegisterUserUseCase registerUserUseCase(
        LoadUserPort loadPort, SaveUserPort savePort,
        Clock clock, PlatformTransactionManager txManager,
        ObservationRegistry registry) {
    var service = new RegisterUserService(loadPort, savePort, clock);
    var txProxy = createTxProxy(service, RegisterUserUseCase.class, txManager);
    // Observation 래핑 (TX 프록시 위에)
    return ObservedProxy.create(txProxy, "identity.user.register", registry);
}
```

---

## 5. Domain 계층 관찰 방법

Domain 모듈에 로깅/메트릭 코드를 두지 않고 Domain 행위를 관찰하는 방법:

| 방법 | 설명 |
|------|------|
| **Domain Event** | 비즈니스 사건을 이벤트로 발행. Adapter가 이벤트 리스너로 로깅/메트릭 수집 |
| **반환값** | UseCase Result DTO 기반으로 Configuration에서 메트릭 기록 |
| **예외** | Domain Exception → Adapter ControllerAdvice에서 로깅 + 메트릭 |
| **TX 프록시** | Configuration의 TX 프록시에 Observation 래핑 추가 |

---

## 6. 컨텍스트 전파 (A-11) — ScopedValue vs ThreadLocal

> 근거: ADR-0012

### A-11 ScopedValue 필수 (ThreadLocal 금지)

- MUST: 비즈니스 컨텍스트(userId, tenantId, permissions) 전파에 `ScopedValue` (Java 25)를 사용한다.
- MUST NOT: `ThreadLocal`을 사용한다. Virtual Thread 환경에서 가상 스레드마다 복사본 생성으로 격리 불안정.
- MUST NOT: Spring Security의 `SecurityContextHolder`(ThreadLocal)를 ScopedValue로 교체한다 (Spring 공식 미지원).

### 전파 흐름

```
HTTP Request (traceparent 헤더)
    │
    ├── OpenTelemetry: W3C Trace Context 파싱 → traceId, spanId → MDC 자동 주입
    ├── Filter (Adapter In): ScopedValue.where(CTX, requestContext).run(...)
    │       → JWT claim 파싱: sub→userId, tid→tenantId, permissions
    └── Spring Security: SecurityContextHolder → Authentication 설정
    ↓
Application Service
    ├── RequestScope.CTX.get() 읽기 (인가 판단, 소유권 검증)
    └── 비즈니스 로직
    ↓
Persistence Adapter (Output)
    └── RequestScope.CTX.get() → 메시지 헤더에 userId/tenantId 주입
```

### ScopedValue 바인딩 (Filter)

```java
// Adapter (In) — Filter
public class RequestContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        var token = extractJwt((HttpServletRequest) req);
        var ctx = new RequestContext(
            new UserId(token.getClaim("sub")),
            new TenantId(token.getClaim("tid")),
            parsePermissions(token)
        );
        ScopedValue.where(RequestScope.CTX, ctx)
            .run(() -> chain.doFilter(req, res));
    }
}
```

### ScopedValue 읽기 (Application Service)

```java
// Application Service — ScopedValue 읽기 (인가 판단, 소유권 검증)
public class DeactivateUserService implements DeactivateUserUseCase {
    public void execute(DeactivateUserCommand cmd) {
        var ctx = RequestScope.CTX.get();
        if (!ctx.permissions().contains(new Permission("user:deactivate"))) {
            throw new AccessDeniedException("user:deactivate");
        }
        var user = loadPort.findById(new UserId(cmd.targetUserId())).orElseThrow();
        user.deactivate(clock.instant());
        savePort.save(user);
    }
}
```

### 주의 사항

- MUST NOT: ScopedValue에 `correlationId`를 별도 포함한다. `traceId`가 대체한다.
- MUST NOT: `CompletableFuture`, `@Async`, `ExecutorService.submit()`에서 ScopedValue를 사용한다. 바인딩이 유실된다.
- SHOULD: ScopedValue가 필요한 비동기는 `StructuredTaskScope`를 사용한다 (자식 스레드에 자동 상속).

---

## 7. traceId를 Domain Event 페이로드에 포함하지 않는 이유

- MUST NOT: `traceId`, `userId`, `tenantId`를 Domain Event 페이로드에 포함한다.
- MUST: OpenTelemetry가 메시지 헤더(W3C Trace Context)로 `traceId`를 자동 전파한다.
- MUST: `userId`, `tenantId`는 Persistence Adapter가 Outbox 메시지 헤더에 주입한다.

**근거**:
1. Domain Event는 비즈니스 도메인만 표현해야 한다 (D-1, D-2).
2. traceId는 인프라 계층(OpenTelemetry)이 처리한다.
3. CloudEvents, W3C 표준에서 traceId, userId는 메시지 속성으로 정의된다.

---

## 8. HealthIndicator

모든 Outbound Adapter별로 독립적인 HealthIndicator를 구현한다.

```java
// Adapter 또는 Configuration 모듈
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final DSLContext dsl;

    public DatabaseHealthIndicator(DSLContext dsl) { this.dsl = dsl; }

    @Override
    public Health health() {
        try {
            dsl.selectOne().fetch();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true    # /actuator/health/liveness + /readiness 활성화
```

| Probe | 엔드포인트 | 실패 시 |
|-------|----------|--------|
| Liveness | `/actuator/health/liveness` | 컨테이너 재시작 |
| Readiness | `/actuator/health/readiness` | 트래픽 차단 |

---

## 9. Metrics

### 자동 수집

| 카테고리 | 예시 메트릭 |
|---------|-----------|
| JVM | `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live` |
| HTTP | `http.server.requests` (count, duration, status) |
| DB Connection Pool | `hikaricp.connections.active` |

### 비즈니스 메트릭 (ObservationRegistry)

```java
// Configuration 모듈에서 UseCase에 Observation 래핑
return ObservedProxy.create(txProxy, "identity.user.register", registry);
```

### SLO 기반 알람 기준

| SLI | SLO | 알람 조건 |
|-----|-----|----------|
| HTTP P99 응답 시간 | < 500ms | P99 > 500ms 5분 연속 |
| HTTP 5xx 에러율 | < 1% | 5xx > 1% 3분 연속 |
| Outbox 미처리 이벤트 | 0건 | completion_date IS NULL > 0, 5분 초과 |

---

## fallback 지시문

---
> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> `docs/decisions/ADR-0012-*.md`(ScopedValue 컨텍스트 전파) 또는 `ADR-0016-*.md`(ECS 구조화 로깅 채택 근거) 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
