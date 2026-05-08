# Strategic Design Changelog

> BC 추가·변경 시 이 파일에 이력을 누적한다.
> ADR 컬럼은 선택 — 중요한 결정이 ADR로 작성된 경우에만 기재한다.

---

| 날짜 | 변경 유형 | 내용 | ADR | 영향 파일 |
|------|---------|------|-----|---------|
| 2026-05-07 | 최초 작성 | Identity BC Core Domain 분류, UL 정의, Context Map | — | context-map.md, ubiquitous-language-identity.md |
| 2026-05-08 | 구조 재편 | docs/architecture → docs/ddd 이동. 단일 파일을 4개 평면 .md로 분리 (UL, 모듈 매핑, 이력 분리) | — | context-map.md, ubiquitous-language-identity.md, module-bc-mapping.md, strategic-design-changelog.md |
| 2026-05-08 | BC 추가 | Notification BC 신설 (Supporting Domain). Identity → Notification 통합: `UserRegisteredIntegrationEvent` ACL 패턴 적용. 5개 Gradle 모듈 추가. notifications 테이블 (V3 DDL). | — | context-map.md, ubiquitous-language-notification.md, module-bc-mapping.md, strategic-design-changelog.md |
