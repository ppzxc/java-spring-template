# spring-vibe-boilerplate

## 프로젝트 개요

사내 프로젝트 시작점. IDP 서버 + Resource Server. Java 25 + Spring Boot 4.0.x 기반.
순수주의 DDD + 헥사고날 아키텍처 + CQRS Level 1 + Spring Modulith 2.0 적용.

## 아키텍처

- **DDD**: Rich Domain Model. Aggregate, VO(record), Domain Event(sealed interface), Domain Exception(sealed class)
- **헥사고날**: 4계층 — Domain → Application → Adapter → Configuration. 의존 방향 항상 바깥→안.
- **CQRS Level 1**: Command/Query 클래스 분리. Load/Save/Query Port 3분할. 동일 DB.
- **Modulith**: Gradle 멀티모듈(컴파일 타임) + Spring Modulith(이벤트 Outbox, 런타임 검증) 하이브리드.

## 모듈 구조

현재 모듈:
- `boilerplate-boot-api`: 전체 조립. Controller, PersistenceAdapter, TX 프록시.
- `boilerplate-shared-event`: BC 간 Integration Event 계약 (Published Language). 순수 Java record.
- `boilerplate-shared-security`: 공통 보안 설정 (JWT Filter 등).
- `boilerplate-test-support`: 테스트 픽스처 공용 인프라 (DomainTestBase, Testcontainers Singleton 등).
- `identity/`: Identity BC 5개 모듈 (Core Domain)
- `notification/`: Notification BC 5개 모듈 (Supporting Domain)
- `audit/`: Audit BC 5개 모듈 (Supporting Domain)

신규 BC 추가 시 모듈 패턴: `boilerplate-{bc}-{layer}`
- `boilerplate-{bc}-domain`
- `boilerplate-{bc}-application`
- `boilerplate-{bc}-adapter-input-api`
- `boilerplate-{bc}-adapter-output-persist`
- `boilerplate-{bc}-configuration`

## 기술 스택

- Java 25 (ScopedValue, Virtual Threads, record, sealed)
- Spring Boot 4.0.5 (libs.versions.toml: org-springframework-boot = "4.0.5")
- Spring Modulith 2.0.x (2.0.1 안정 버전, Spring Boot 4 BOM 연동)
- jOOQ (Adapter 계층, Domain 순수성 보호)
- Gradle 멀티모듈 + linecorp build-recipe-plugin
- JUnit 5, AssertJ, Testcontainers, ArchUnit, Fixture Monkey

## 규칙

`.claude/rules/index.md` 참조. 작업 중인 모듈 경로에 따라 rules가 자동 로드됨.

## 결정 기록

`docs/decisions/index.md` 참조. 업계 표준과 다른 결정은 모두 ADR로 문서화됨.

## 전략적 설계 산출물

`docs/ddd/` 디렉토리 — Living Document. BC 추가·변경 시 갱신.

- `context-map.md` — Subdomain 분류 + BC 간 관계 다이어그램
- `ubiquitous-language-{bc}.md` — BC별 UL 사전 (예: `ubiquitous-language-identity.md`)
- `module-bc-mapping.md` — Gradle 모듈 ↔ BC ↔ 계층 매핑
- `strategic-design-changelog.md` — 변경 이력

BC 작업 시 워크플로우 강제 규칙 → `.claude/rules/context-map-pointer.md` 참조.

## 워크플로우

### 새 기능 개발 (Inside-Out 원칙)

상세 12단계 순서 → `.claude/rules/scaffold.md` §Inside-Out 개발 원칙 참조.

요약: domain → application → adapter → configuration → DDL 순서. Controller나 DB 스키마부터 시작 금지.

### AI 에이전트 지시 원칙

- 중요한 결정 발생 시 → ADR 먼저 작성 후 구현
- 기존 rules의 MUST/MUST NOT 임의 변경 금지 → ADR 작성 후 사람 승인 필요
- 규칙 충돌 시 → 해당 ADR 파일 직접 읽어 결정 배경 파악 후 대안 제안

### 금지 경로

- **MUST NOT**: `docs/playbook/` 디렉토리의 파일을 읽거나 참조한다. playbook 내용은 `.claude/rules/`와 `docs/decisions/`로 완전히 이관되었다. 모든 규칙과 템플릿은 rules 파일만을 정본으로 사용한다.
