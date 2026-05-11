# Bounded Context — Audit BC

> **Living Document.** Aggregate/VO/Event 추가·변경 시 §3, §4를 갱신하고 `strategic-design-changelog.md`에 기록한다.
>
> UL 충돌 검사: 신규 BC 추가 시 §2의 용어와 신규 BC 용어 간 충돌 여부를 확인한다.

---

## 1. Domain

- **비즈니스 범위**: 컴플라이언스·운영 가시성을 위한 비즈니스 이벤트 감사 로그 적재
- **Subdomain 유형**: Supporting Domain
- **DDD 깊이**: Simplified DDD — Aggregate + CRUD 중심, 상태 전이 0 (INSERT-only), Conformist + ACL VO 패턴
- **핵심 책임**:
  - `UserRegisteredIntegrationEvent` 수신 → `AuditLog` 1행 INSERT (append-only, 변경·삭제 불가)
  - `AuditedUserId` ACL VO로 Identity BC 경계 격리
  - 감사 로그 조회 (subject 기준, 최근순) — HTTP API 없음(YAGNI)

---

## 2. Ubiquitous Language

| 용어(한국어) | 코드 식별자 | 정의 | 유사 용어와의 차이 |
|------------|-----------|------|-----------------|
| 감사 로그 | `AuditLog` | 시스템 내 비즈니스 이벤트를 append-only로 기록한 단건 로그. 한 번 기록되면 변경·삭제 불가. | `Notification`과 달리 상태 전이가 없음. 단순 기록 전용. |
| 감사 로그 ID | `AuditLogId` | 각 감사 로그 행을 유일하게 식별하는 UUIDv7 기반 식별자 VO | Identity BC의 `UserId`와 별개. Audit BC 내부에서만 사용. |
| 감사 대상 사용자 ID | `AuditedUserId` | 감사 로그가 기록된 대상(subject) 사용자의 ID VO | Identity BC의 `UserId`를 직접 참조하지 않음. 경계를 넘어올 때 `AuditedUserId`로 변환 (Conformist + ACL). |
| 감사 이벤트 종류 | `AuditEventType` | 감사 로그가 어떤 비즈니스 이벤트에 의해 생성되었는지 나타내는 열거형. 현재: `USER_REGISTERED`. | `event_type` 컬럼에 VARCHAR(50)으로 저장. 신규 이벤트 추가 시 enum 항목 추가 + listener 추가. |
| 감사 페이로드 | `AuditPayload` | 감사 이벤트의 세부 내용을 담은 JSON 문자열 VO. 1~10,000자 범위. | PostgreSQL JSONB 컬럼에 저장. 스키마 변경 없이 페이로드 필드만 확장 가능. |
| 발생 시각 | `occurredAt` | 원래 비즈니스 이벤트가 발생한 UTC 시각 (`Instant`). Integration Event의 `occurredAt` 그대로 보존. | `recordedAt`(Audit BC 적재 시각)과 구분. |
| 기록 시각 | `recordedAt` | Audit BC가 감사 로그를 DB에 기록한 UTC 시각 (`Instant`). | `occurredAt`(이벤트 발생 시각)과 구분. 재전송 등으로 두 값이 다를 수 있음. |
| 감사 적재 유스케이스 | `RecordUserRegisteredAuditUseCase` | `UserRegisteredIntegrationEvent` 수신 후 `audit_log` 테이블에 1행 INSERT. void 반환. | 조회 UseCase와 달리 Command Side. |
| 주제별 감사 조회 유스케이스 | `FindAuditLogsBySubjectUseCase` | 특정 사용자 ID 기준 감사 로그 목록 조회. | 전체 최근순 조회 UseCase와 달리 대상 사용자 기준. |
| 최근 감사 조회 유스케이스 | `ListRecentAuditLogsUseCase` | 전체 감사 로그를 최근순으로 조회. `limit` 상한: 1~1,000. | subject 기준 조회 UseCase와 달리 전체 대상 최근순. |

---

## 3. Entity & Value Object

### Entity

| 이름 | 종류 | 식별자 | 설명 |
|------|------|--------|------|
| `AuditLog` | Aggregate Root | `AuditLogId` (UUIDv7) | append-only. `create()` 이후 상태 변경 메서드 없음. UPDATE 경로 없음. |

### Value Object

| 이름 | 불변 규칙 / 자기검증 |
|------|-------------------|
| `AuditLogId` | UUID not null |
| `AuditedUserId` | UUID not null (Identity BC UserId의 ACL VO) |
| `AuditPayload` | not null, 1~10,000자 JSON 문자열 |

### Enum

| 이름 | 값 | 전이 제약 |
|------|-----|---------|
| `AuditEventType` | `USER_REGISTERED` | 전이 없음 — 생성 시 확정 |

---

## 4. Aggregate & Aggregate Root

### AuditLog Aggregate

- **Aggregate Root**: `AuditLog`
- **포함 Entity**: 없음 (내부 Entity 없음)
- **포함 VO**: `AuditLogId`, `AuditedUserId`, `AuditPayload`
- **외부 참조**: 없음 (AuditedUserId는 Identity BC UserId의 ACL VO)
- **불변식**:
  - `create()` 이후 상태 변경 불가 (append-only)
  - UPDATE 경로 없음 → Optimistic Lock 미적용 (INSERT-only 설계)
  - `occurredAt`은 Identity BC 이벤트에서 그대로 보존 (재해석 금지)
- **발행 Domain Event**: 없음 (상태 전이가 없으므로 Domain Event 없음)
- **행위 메서드**:
  - `create(...)` — 신규 감사 로그 생성 팩토리
  - `reconstitute(...)` — DB 복원 (이벤트 미발행)
  - `pullDomainEvents()` — 이벤트 수거 후 내부 비움 (항상 빈 목록 반환)

---

## 5. Bounded Context

- **Subdomain**: Supporting Domain (Downstream)
- **하위/상위 관계**: Identity(Upstream) → Audit(Downstream)
- **통합 패턴**:
  - Customer-Supplier: Identity가 변경하면 Audit이 영향받음
  - Conformist: `UserRegisteredIntegrationEvent`를 수정 없이 수용 (shared-event 모듈 무수정 참조)
  - ACL (Anti-Corruption Layer): `UserRegisteredIntegrationEvent.userId` → `AuditedUserId` (ACL VO 변환)
- **구독 Integration Event** (`boilerplate-shared-event/identity/`):
  - `UserRegisteredIntegrationEvent` — Identity BC에서 발행
- **발행 Integration Event**: 없음
- **HTTP API**: 없음 (YAGNI — 관리자 콘솔 요구 시 `adapter-input-api` 모듈 추가)
- **Gradle 모듈**:
  - `boilerplate-audit-domain` — Domain 계층 (순수 Java, append-only)
  - `boilerplate-audit-application` — Application 계층 (Port, UseCase, Command/Query/Result)
  - `boilerplate-audit-adapter-input-event` — `@ApplicationModuleListener` 이벤트 핸들러
  - `boilerplate-audit-adapter-output-persist` — jOOQ PersistenceAdapter (INSERT-only), QueryAdapter, Mapper
  - `boilerplate-audit-configuration` — Bean 등록, TX 프록시
