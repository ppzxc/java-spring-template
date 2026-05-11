# Context Map — Boilerplate System

> **Living Document.** BC 추가·변경 시 갱신한다.
>
> BC별 상세 정보: `bounded-context-{bc}.md` (UL, Entity/VO, Aggregate, BC 경계 포함)
> 결정의 배경: `docs/decisions/`(ADR)
> 변경 이력: `strategic-design-changelog.md`

---

## 1. Subdomain 분류

| Bounded Context | Subdomain 유형 | DDD 깊이 | 이유 |
|----------------|--------------|---------|------|
| **Identity BC** | Core Domain | Full DDD | 사내 IDP + Resource Server의 핵심 차별화 요소. 인증/인가 정책이 비즈니스 그 자체. 없으면 서비스가 성립하지 않는다. Rich Domain Model, Aggregate, VO(record), Domain Event(sealed interface), Domain Exception(sealed class), Output Port 3분할(Load/Save/Query). |
| **Notification BC** | Supporting Domain | Simplified DDD | 알림 적재·발송은 비즈니스를 지원하는 공통 인프라. 경쟁 우위 요소가 아니며 외부 채널(SMTP, SMS 등) 연동 확장을 위한 ACL 경계로 분리. Aggregate + CRUD 중심, 상태 전이 규칙은 단순(PENDING → SENT/FAILED). |
| **Audit BC** | Supporting Domain | Simplified DDD | 감사 로그 적재는 컴플라이언스/운영 가시성을 지원하는 인프라. 비즈니스 차별화 요소가 아니며, Identity 이벤트를 Conformist 방식으로 수용해 append-only로 적재. 상태 전이 0, INSERT만 수행. |

### Generic Subdomain (현재 없음)

아래 후보들은 필요 시점에 ADR을 거쳐 도입한다. **후보가 곧 BC 분리를 의미하지 않음.**

| 후보 | 적합한 외부 솔루션 | 도입 시 BC 위치 |
|------|-----------------|---------------|
| Email Delivery | SendGrid / AWS SES | `notification-channel-email` 또는 별도 BC |
| File Storage | S3 / MinIO | `boilerplate-storage` BC |
| Payment | Stripe / PortOne | `boilerplate-payment` BC |
| Search | Elasticsearch / OpenSearch | `boilerplate-search` BC |

---

## 2. Bounded Context 다이어그램

```
┌─────────────────────────────┐     Published Language      ┌──────────────────────────┐
│        Identity BC          │ ─────────────────────────→ │     Notification BC      │
│   (Core Domain, Upstream)   │  UserRegisteredIntegration  │  (Supporting, Downstream)│
│                             │  Event                      │   ACL 패턴 적용           │
│  User, Role, Permission,    │                             │                          │
│  Client, Credential         │                             │  Notification, Channel,  │
└─────────────────────────────┘                             │  Status, RecipientUserId │
              │                                             └──────────────────────────┘
              │ Published Language
              │ (shared-event 모듈)
              │
              │ UserRegisteredIntegrationEvent
              │ (Conformist + ACL VO)
              ↓
┌─────────────────────────────┐
│         Audit BC            │
│   (Supporting, Downstream)  │
│   Conformist + ACL VO       │
│                             │
│  AuditLog, AuditedUserId,   │
│  AuditEventType, AuditPayload│
└─────────────────────────────┘

              │ Published Language 컨테이너
              ↓
┌─────────────────────────────┐
│  boilerplate-shared-event   │
│  (Published Language 컨테이너)│
│                             │
│  IntegrationEvent (marker)  │
│  UserRegisteredIntegration  │
│  Event                      │
└─────────────────────────────┘
```

---

## 3. 관계 패턴 6종

| 패턴 | 정의 | 본 프로젝트 사례 |
|------|------|---------------|
| **Shared Kernel** | 두 BC가 동일 모델을 공유. 변경 시 양측 합의 필수. 최소화 원칙. | `boilerplate-shared-event` — Integration Event 계약만 공유 (최소 Shared Kernel, Published Language 변형) |
| **Customer-Supplier** | Downstream(D)이 Upstream(U)의 변경에 영향받음. D가 요구사항 전달, U가 수용. | Identity(U) → Audit(D), Identity(U) → Notification(D) |
| **Conformist** | D가 U 모델을 그대로 수용. D는 번역 없이 U 모델에 종속됨. | Audit BC는 `UserRegisteredIntegrationEvent`를 수정 없이 수용 |
| **Anti-Corruption Layer (ACL)** | D가 U 모델을 자체 모델로 번역하여 격리. | Notification BC: `UserRegisteredIntegrationEvent.userId` → `RecipientUserId` (ACL VO). Audit BC: `userId` → `AuditedUserId` (ACL VO) |
| **Open Host Service (OHS)** | U가 표준 프로토콜(REST/gRPC)로 서비스를 제공. D가 직접 호출. | Identity BC REST API (`/api/identity/users`, `/api/identity/auth`) — 외부 클라이언트에 OHS 제공 |
| **Published Language** | BC 간 합의된 메시지 포맷. 버전 호환 이벤트 계약. | `boilerplate-shared-event` 모듈 — `UserRegisteredIntegrationEvent` record |

---

## 4. DDD 5단계 모델링 흐름

신규 BC 추가 시 아래 5단계를 완료한 후 `scaffold.md` 패턴으로 구현한다.
각 단계의 상세 절차는 `strategic-design.md`를 참조한다.

```
[Step 1] 도메인 탐구 (Domain Discovery)
  산출물: bounded-context-{bc}.md § 1. Domain
  ↓
[Step 2] Ubiquitous Language 수립
  산출물: bounded-context-{bc}.md § 2. Ubiquitous Language
  ↓
[Step 3] Entity & Value Object 식별
  산출물: bounded-context-{bc}.md § 3. Entity & Value Object
  ↓
[Step 4] Aggregate & Aggregate Root 설계
  산출물: bounded-context-{bc}.md § 4. Aggregate & Aggregate Root
  ↓
[Step 5] Bounded Context 확정 + Subdomain 분류 + DDD 깊이
  산출물: bounded-context-{bc}.md § 5. Bounded Context
           + context-map.md § 1 표 + § 2 다이어그램 + § 5 모듈 매핑 갱신
```

---

## 5. 모듈 ↔ BC ↔ 계층 매핑

> Gradle 모듈, BC, 계층의 매핑 매트릭스.
> 신규 BC 추가 시 `scaffold.md §신규 BC 모듈 초기화 체크리스트 Step 7`과 함께 갱신한다.

### Identity BC

| Gradle 모듈 | 계층 | 역할 |
|------------|------|------|
| `boilerplate-identity-domain` | Domain | Aggregate(User, Credential), VO(UserId, Email, UserName, HashedPassword, AccessToken, RefreshToken, TokenSet), Event(UserEvent), Exception(UserException) — 순수 Java |
| `boilerplate-identity-application` | Application | Port(LoadUserPort, SaveUserPort, UserQueryPort, …), UseCase, Command/Query/Result |
| `boilerplate-identity-adapter-input-api` | Adapter-In | REST Controller, Request/Response DTO |
| `boilerplate-identity-adapter-output-persist` | Adapter-Out | jOOQ PersistenceAdapter, QueryAdapter, Mapper |
| `boilerplate-identity-configuration` | Configuration | Bean 등록, TX 프록시, EventTranslator |

### Notification BC

| Gradle 모듈 | 계층 | 역할 |
|------------|------|------|
| `boilerplate-notification-domain` | Domain | Aggregate(Notification), VO(NotificationId, RecipientUserId, NotificationContent), Enum(NotificationChannel, NotificationStatus) — 순수 Java |
| `boilerplate-notification-application` | Application | Port(Load/Save/Query), UseCase, Command/Result |
| `boilerplate-notification-adapter-input-event` | Adapter-In | `@ApplicationModuleListener` 이벤트 핸들러(IdentityUserEventHandler) |
| `boilerplate-notification-adapter-output-persist` | Adapter-Out | jOOQ PersistenceAdapter, QueryAdapter, Mapper |
| `boilerplate-notification-configuration` | Configuration | Bean 등록, TX 프록시 |

### Audit BC

| Gradle 모듈 | 계층 | 역할 |
|------------|------|------|
| `boilerplate-audit-domain` | Domain | Aggregate(AuditLog), VO(AuditedUserId, AuditLogId, AuditPayload), Enum(AuditEventType) — 순수 Java, append-only |
| `boilerplate-audit-application` | Application | Port(Load/Save/Query), UseCase(Record/Find/List), Command/Query/Result |
| `boilerplate-audit-adapter-input-event` | Adapter-In | `@ApplicationModuleListener` 이벤트 핸들러(IdentityUserRegisteredEventHandler) |
| `boilerplate-audit-adapter-output-persist` | Adapter-Out | jOOQ PersistenceAdapter(INSERT-only), QueryAdapter, Mapper — audit_log 테이블 |
| `boilerplate-audit-configuration` | Configuration | Bean 등록, TX 프록시 |

### Cross-cutting 모듈 (BC 아님)

| Gradle 모듈 | 역할 | 비고 |
|------------|------|------|
| `boilerplate-shared-event` | BC 간 Integration Event 계약 — 순수 Java record | Published Language 컨테이너. DomainEvent 아님 |
| `boilerplate-shared-security` | JWT Filter, RequestScope, ScopedValue 바인딩 | cross-cutting 보안 인프라. BC에 속하지 않음 |
| `boilerplate-test-support` | DomainTestBase, AdapterTestBase, Fixture Factory, Testcontainers Singleton | 테스트 공용 인프라. BC에 속하지 않음 |
| `boilerplate-boot` | Spring Boot 진입점, 전체 조립, ArchUnit/Modulith 검증 | All BC Configuration 조립. BootJar 생성 |
