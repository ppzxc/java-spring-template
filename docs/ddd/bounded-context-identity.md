# Bounded Context — Identity BC

> **Living Document.** Aggregate/VO/Event 추가·변경 시 §3, §4를 갱신하고 `strategic-design-changelog.md`에 기록한다.
>
> UL 충돌 검사: 신규 BC 추가 시 §2의 용어와 신규 BC 용어 간 충돌 여부를 확인한다.

---

## 1. Domain

- **비즈니스 범위**: 시스템의 인증·인가를 담당하는 사내 IDP(Identity Provider) + Resource Server
- **Subdomain 유형**: Core Domain
- **DDD 깊이**: Full DDD — Rich Domain Model, Aggregate, VO(record), Domain Event(sealed interface), Domain Exception(sealed class), Output Port 3분할(Load/Save/Query)
- **핵심 책임**:
  - 사용자(User) 등록, 조회, 상태 전이(ACTIVE → SUSPENDED → DEACTIVATED)
  - 자격증명(Credential) 관리 — BCrypt/Argon2 해시 저장, 비밀번호 검증
  - Role / Permission 기반 인가 정책 정의
  - OAuth2 Client 관리 및 Token(Access/Refresh) 발급
  - 인증 이벤트를 다른 BC(Notification, Audit)에 발행

---

## 2. Ubiquitous Language

| 용어(한국어) | 코드 식별자 | 정의 | 유사 용어와의 차이 |
|------------|-----------|------|-----------------|
| 사용자 | `User` | 시스템에 등록된 식별 가능한 주체 | `Member`(팀 소속 관계)와 다름 |
| 사용자 ID | `UserId` | UUIDv7 식별자 VO | 단순 `UUID`와 구분 — VO 래핑, 타입 안전성 보장 |
| 이메일 | `Email` | 로그인 식별자 겸 알림 채널 | 단순 `String` 아님 — RFC 5322 형식 검증 |
| 사용자 이름 | `UserName` | 표시용 이름 (1-50자 제약) | Identifier 아님, Display only |
| 해시된 비밀번호 | `HashedPassword` | BCrypt/Argon2 해시 결과 | 평문 Password는 도메인에 절대 존재하지 않음 |
| 자격증명 | `Credential` | User Aggregate 내부 Entity (해시된 비밀번호 + 만료 정보 포함) | Aggregate Root는 `User`, `Credential`은 종속 Entity |
| 사용자 상태 | `UserStatus` | `ACTIVE` / `SUSPENDED` / `DEACTIVATED` enum | 단방향 전이만 허용 (DEACTIVATED → ACTIVE 불가) |
| 권한 | `Permission` | `resource:scope` 형식 VO (예: `user:create`) | OAuth2 `Scope`와 다름 — Permission은 최소 인가 단위 |
| 역할 | `Role` | `Permission` 집합 Aggregate | `Group`(조직 단위)과 다름 — 순수 권한 집합 |
| 클라이언트 | `Client` | OAuth2 클라이언트 애플리케이션 | `User`(사람)와 구분 — M2M 토큰 발급 주체 |
| 액세스 토큰 | `AccessToken` | JWT 기반 단기 인증 토큰 VO | `RefreshToken`(장기, 갱신용)과 구분 |
| 갱신 토큰 | `RefreshToken` | 장기 유효 토큰 VO — Access Token 재발급에 사용 | `AccessToken`과 달리 API 호출에 직접 사용 불가 |
| 토큰 세트 | `TokenSet` | AccessToken + RefreshToken 쌍 VO | 개별 토큰 단건이 아닌 로그인 결과 묶음 |

---

## 3. Entity & Value Object

### Entity

| 이름 | 종류 | 식별자 | 설명 |
|------|------|--------|------|
| `User` | Aggregate Root | `UserId` (UUIDv7) | 사용자 생명주기 전체 관리. `Set<RoleId>`로 Role ID 참조 |
| `Credential` | 내부 Entity | `CredentialId` (UUIDv7) | User Aggregate 내부. 비밀번호 해시 + 만료. package-private `create()`/`reconstitute()` |

### Value Object

| 이름 | 불변 규칙 / 자기검증 |
|------|-------------------|
| `UserId` | UUID not null |
| `Email` | not null, RFC 5322 형식 |
| `UserName` | not null, 1-50자 |
| `HashedPassword` | not null, 공백 불가 |
| `AccessToken` | not null |
| `RefreshToken` | not null |
| `TokenSet` | AccessToken + RefreshToken 모두 not null |

### Enum

| 이름 | 값 | 전이 제약 |
|------|-----|---------|
| `UserStatus` | `ACTIVE`, `SUSPENDED`, `DEACTIVATED` | DEACTIVATED → 다른 상태 전이 불가 |

---

## 4. Aggregate & Aggregate Root

### User Aggregate

- **Aggregate Root**: `User`
- **포함 Entity**: `Credential` (내부 Entity, package-private 접근)
- **포함 VO**: `UserId`, `Email`, `UserName`, `HashedPassword`, `TokenSet`
- **외부 참조**: `Set<RoleId>` — ID만 참조 (D-9)
- **불변식**:
  - DEACTIVATED 상태의 User는 재활성화 불가
  - Email은 시스템 전체에서 유일 (중복 검사는 Application)
  - 비밀번호 평문은 Domain에 절대 존재하지 않음
- **발행 Domain Event** (`UserEvent` sealed interface):
  - `UserRegisteredEvent` — 신규 등록 시
  - `UserLoggedInEvent` — 로그인 성공 시
  - (추가 이벤트는 `UserEvent permits` 갱신)
- **행위 메서드**:
  - `register(...)` / `create(...)` — 신규 생성 팩토리
  - `reconstitute(...)` — DB 복원 (이벤트 미발행)
  - `login(password, clock)` — 비밀번호 검증 + 토큰 발급
  - `pullDomainEvents()` — 이벤트 수거 후 내부 비움

---

## 5. Bounded Context

- **Subdomain**: Core Domain (Upstream)
- **하위/상위 관계**:
  - Identity(Upstream) → Notification(Downstream): Customer-Supplier
  - Identity(Upstream) → Audit(Downstream): Customer-Supplier
- **통합 패턴**:
  - OHS(Open Host Service): REST API (`/api/identity/users`, `/api/identity/auth`) 제공
  - Published Language: `boilerplate-shared-event` 모듈을 통해 Integration Event 발행
- **발행 Integration Event** (`boilerplate-shared-event/identity/`):
  - `UserRegisteredIntegrationEvent` — 사용자 등록 완료 시
- **구독 Integration Event**: 없음 (Upstream)
- **Gradle 모듈**:
  - `boilerplate-identity-domain` — Domain 계층 (순수 Java)
  - `boilerplate-identity-application` — Application 계층 (Port, UseCase, Command/Query/Result)
  - `boilerplate-identity-adapter-input-api` — REST Controller, Request/Response DTO
  - `boilerplate-identity-adapter-output-persist` — jOOQ PersistenceAdapter, QueryAdapter, Mapper
  - `boilerplate-identity-configuration` — Bean 등록, TX 프록시, EventTranslator
