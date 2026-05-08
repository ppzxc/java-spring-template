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
