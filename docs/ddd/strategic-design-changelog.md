# Strategic Design Changelog

> BC 추가·변경 시 이 파일에 이력을 누적한다.
> ADR 컬럼은 선택 — 중요한 결정이 ADR로 작성된 경우에만 기재한다.

---

| 날짜 | 변경 유형 | 내용 | ADR | 영향 파일 |
|------|---------|------|-----|---------|
| 2026-05-07 | 최초 작성 | Identity BC Core Domain 분류, UL 정의, Context Map | — | context-map.md, ubiquitous-language-identity.md |
| 2026-05-08 | 구조 재편 | docs/architecture → docs/ddd 이동. 단일 파일을 4개 평면 .md로 분리 (UL, 모듈 매핑, 이력 분리) | — | context-map.md, ubiquitous-language-identity.md, module-bc-mapping.md, strategic-design-changelog.md |
| 2026-05-08 | BC 추가 | Notification BC 신설 (Supporting Domain). Identity → Notification 통합: `UserRegisteredIntegrationEvent` ACL 패턴 적용. 5개 Gradle 모듈 추가. notifications 테이블 (V3 DDL). | — | context-map.md, ubiquitous-language-notification.md, module-bc-mapping.md, strategic-design-changelog.md |
| 2026-05-08 | BC 추가 | Audit BC 신설 (Supporting Domain). Identity → Audit Conformist+ACL VO 패턴. 5개 Gradle 모듈 추가. audit_log 테이블 JSONB + subject_user_id 인덱스 (V4 DDL). HTTP API 없음(YAGNI). | — | context-map.md, ubiquitous-language-audit.md, module-bc-mapping.md, strategic-design-changelog.md |
| 2026-05-08 | Module Cleanup | `boilerplate-domain`, `boilerplate-application` 빈 모듈 제거. 두 모듈은 `package-info.java` 1개씩만 보유하며 어떤 BC 모듈도 의존하지 않았음. ADR-0003 "Common 모듈 금지 + `boilerplate-{bc}-{layer}` 네이밍 패턴" 원칙에 따라 BC 비소속 도메인/애플리케이션 모듈은 부재가 정상 상태. | ADR-0003 | settings.gradle.kts, boilerplate-boot-api/build.gradle.kts |
| 2026-05-09 | Module Rename | `boilerplate-boot-api` → `boilerplate-boot` 리네임. 이 모듈은 REST API 전용 모듈이 아니라 전체 조립+보안+관측 인프라를 포함하는 Spring Boot 진입점이므로 `-api` 접미어가 오해를 유발. 향후 `boilerplate-boot-batch` 등 비-API 진입점 추가 시 일관된 네임스페이스 확보. Java 패키지(`*.boot.*`)는 이미 정렬되어 소스 변경 없음. | — | settings.gradle.kts, .github/workflows/ci.yml, .github/workflows/release.yml, lefthook.yml, .claude/rules/* (7개), .claude/CLAUDE.md, docs/decisions/0017-jib-container-build.md, docs/ddd/module-bc-mapping.md, README.md |
| 2026-05-11 | docs/ddd 재구성 | (1) `module-bc-mapping.md` 삭제 → `context-map.md §5`로 통합. (2) `ubiquitous-language-{identity,notification,audit}.md` → `bounded-context-{identity,notification,audit}.md` rename + 5섹션 템플릿(Domain/UL/Entity·VO/Aggregate/BC) 적용. (3) `context-map.md` §1 Generic Subdomain 슬롯 + 후보 4종 신설. (4) `context-map.md §3` 관계 패턴 6종 상세 신설. (5) `context-map.md §4` DDD 5단계 흐름 신설. (6) `.claude/rules/` 7대 규칙 + 5단계 모델링 반영 (domain.md Entity vs VO 표, Aggregate Root 4역할 표, strategic-design.md 7단계 재구성, scaffold.md Step 0 DDD 게이트 신설, context-map-pointer.md §2 5단계 매핑 표). (7) ArchUnit `BoundedContextStructureTest.java` 10개 테스트 신설. | — | context-map.md, bounded-context-identity.md, bounded-context-notification.md, bounded-context-audit.md, strategic-design-changelog.md, .claude/rules/domain.md, .claude/rules/strategic-design.md, .claude/rules/scaffold.md, .claude/rules/context-map-pointer.md, .claude/CLAUDE.md, BoundedContextStructureTest.java |
