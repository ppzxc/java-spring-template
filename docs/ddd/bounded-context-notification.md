# Bounded Context — Notification BC

> **Living Document.** Aggregate/VO/Event 추가·변경 시 §3, §4를 갱신하고 `strategic-design-changelog.md`에 기록한다.
>
> UL 충돌 검사: 신규 BC 추가 시 §2의 용어와 신규 BC 용어 간 충돌 여부를 확인한다.

---

## 1. Domain

- **비즈니스 범위**: 알림 메시지의 생성·적재·발송 상태 관리
- **Subdomain 유형**: Supporting Domain
- **DDD 깊이**: Simplified DDD — Aggregate + CRUD 중심, 상태 전이 규칙 단순 (PENDING → SENT/FAILED), ACL 패턴으로 Identity BC와 격리
- **핵심 책임**:
  - `UserRegisteredIntegrationEvent` 수신 → `Notification` PENDING 상태로 적재
  - 알림 채널(EMAIL/SMS/PUSH) 별 발송 상태 추적
  - 외부 채널 연동 확장을 위한 ACL 경계 제공 (RecipientUserId VO)

---

## 2. Ubiquitous Language

| 용어(한국어) | 코드 식별자 | 정의 | 유사 용어와의 차이 |
|------------|-----------|------|-----------------|
| 알림 | `Notification` | 특정 수신자에게 발송 예정인 메시지 단위 (채널, 상태, 내용 포함) | Identity BC의 `User`와 다름 — Notification은 발송 행위와 그 결과를 표현 |
| 알림 ID | `NotificationId` | UUIDv7 식별자 VO | 단순 `UUID`와 구분 — VO 래핑, 타입 안전성 보장 |
| 수신자 사용자 ID | `RecipientUserId` | 알림을 수신할 대상의 사용자 ID VO | Identity BC의 `UserId`와 의미는 같으나 Notification BC 경계 내 자체 VO로 정의 (ACL 패턴) |
| 알림 채널 | `NotificationChannel` | 발송 수단 enum: `EMAIL`, `SMS`, `PUSH` | 발송 채널을 코드로 고정 — 동적 확장 시 enum 항목 추가 |
| 알림 상태 | `NotificationStatus` | `PENDING`(발송 대기) / `SENT`(발송 완료) / `FAILED`(발송 실패) enum | `PENDING`이 초기 상태 |
| 알림 내용 | `NotificationContent` | subject(제목, 최대 200자) + body(본문, 최대 5000자) VO | 단순 `String` 아님 — 길이 제약 포함한 자기검증 VO |
| 알림 생성 이벤트 | `NotificationCreatedEvent` | Notification Aggregate 생성 시 발행되는 Domain Event | BC 내부에서만 사용 — Integration Event 아님 |
| 회원가입 알림 발송 유스케이스 | `SendUserRegisteredNotificationUseCase` | `UserRegisteredIntegrationEvent` 수신 시 PENDING 상태 알림을 notifications 테이블에 적재 | 실제 외부 발송(SMTP 등)은 별도 UseCase로 확장 예정 |

---

## 3. Entity & Value Object

### Entity

| 이름 | 종류 | 식별자 | 설명 |
|------|------|--------|------|
| `Notification` | Aggregate Root | `NotificationId` (UUIDv7) | 알림 생명주기 관리. 채널·상태·내용 캡슐화 |

### Value Object

| 이름 | 불변 규칙 / 자기검증 |
|------|-------------------|
| `NotificationId` | UUID not null |
| `RecipientUserId` | UUID not null (Identity BC의 UserId를 ACL VO로 격리) |
| `NotificationContent` | subject not null (1-200자), body not null (1-5000자) |

### Enum

| 이름 | 값 | 전이 제약 |
|------|-----|---------|
| `NotificationChannel` | `EMAIL`, `SMS`, `PUSH` | 전이 없음 — 생성 시 확정 |
| `NotificationStatus` | `PENDING`, `SENT`, `FAILED` | PENDING이 초기 상태 |

---

## 4. Aggregate & Aggregate Root

### Notification Aggregate

- **Aggregate Root**: `Notification`
- **포함 Entity**: 없음 (내부 Entity 없음)
- **포함 VO**: `NotificationId`, `RecipientUserId`, `NotificationContent`
- **외부 참조**: 없음 (RecipientUserId는 Identity BC UserId의 ACL VO)
- **불변식**:
  - 생성 시 채널과 수신자는 변경 불가
  - PENDING 상태에서만 SENT/FAILED 전이 가능 (상태 전이 메서드 추후 구현)
- **발행 Domain Event**:
  - `NotificationCreatedEvent` — Notification 생성 시
- **행위 메서드**:
  - `create(...)` — 신규 알림 생성 팩토리 (PENDING 상태)
  - `reconstitute(...)` — DB 복원 (이벤트 미발행)
  - `pullDomainEvents()` — 이벤트 수거 후 내부 비움

---

## 5. Bounded Context

- **Subdomain**: Supporting Domain (Downstream)
- **하위/상위 관계**: Identity(Upstream) → Notification(Downstream)
- **통합 패턴**:
  - Customer-Supplier: Identity가 변경하면 Notification이 영향받음
  - ACL (Anti-Corruption Layer): `UserRegisteredIntegrationEvent.userId` → `RecipientUserId` (ACL VO 변환)
- **구독 Integration Event** (`boilerplate-shared-event/identity/`):
  - `UserRegisteredIntegrationEvent` — Identity BC에서 발행
- **발행 Integration Event**: 없음
- **Gradle 모듈**:
  - `boilerplate-notification-domain` — Domain 계층 (순수 Java)
  - `boilerplate-notification-application` — Application 계층 (Port, UseCase, Command/Result)
  - `boilerplate-notification-adapter-input-event` — `@ApplicationModuleListener` 이벤트 핸들러
  - `boilerplate-notification-adapter-output-persist` — jOOQ PersistenceAdapter, QueryAdapter, Mapper
  - `boilerplate-notification-configuration` — Bean 등록, TX 프록시
