---
description: BC 추가 전략적 설계 파이프라인 — Subdomain 분류, UL, Context Map, DDD 깊이 결정
alwaysApply: true
---

# 전략적 설계 규칙

신규 Bounded Context(BC) 추가 시 전략적 설계 파이프라인 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 신규 BC 추가 파이프라인

신규 BC를 추가하기 전에 반드시 아래 7단계를 순서대로 수행한다. **구현(scaffold.md §Step 0) 시작 전에 완료**한다.
각 단계 산출물은 `bounded-context-{bc}.md` 해당 섹션에 기록한다.

```
[Step 1] 도메인 탐구 (Domain Discovery)
     산출물: bounded-context-{bc}.md §1 Domain
     활동: 비즈니스 범위, 핵심 책임, Subdomain 유형, DDD 깊이 결정
     ↓
[Step 2] Ubiquitous Language(UL) 수립
     산출물: bounded-context-{bc}.md §2 Ubiquitous Language
     강제: 다른 BC의 bounded-context-*.md §2와 용어 충돌 검사 필수
     ↓
[Step 3] Entity & Value Object 식별
     산출물: bounded-context-{bc}.md §3 Entity & Value Object
     강제: Entity vs VO 결정 기준 표 (domain.md) 적용
     ↓
[Step 4] Aggregate & Aggregate Root 설계
     산출물: bounded-context-{bc}.md §4 Aggregate & Aggregate Root
     강제: Aggregate Root 4역할 (유일 진입점/일관성 경계/이벤트 발행/ID 참조) 확인
           1 TX = 1 Aggregate 원칙 설계
     ↓
[Step 5] Bounded Context 확정 + Subdomain 분류 + 통합 패턴 결정
     산출물: bounded-context-{bc}.md §5 + context-map.md §1, §2, §5 갱신
     강제: Subdomain (Core/Supporting/Generic) 판단 + 통합 패턴 (ACL/Conformist/OHS) 결정
     ↓
[Step 6] ADR 작성 (중요한 결정 시)
     대상: Subdomain 분류 / BC 경계 / 통합 패턴 / DDD 깊이 중 하나라도 결정한 경우
     ↓
[Step 7] scaffold.md §Step 0 DDD 게이트 확인 후 구현 시작
```

- MUST: Step 1 완료 전에 Step 2로 진행하지 않는다.
- MUST: Step 5 완료 전에 scaffold.md §Step 0 체크리스트 통과 없이 Gradle 모듈을 생성하지 않는다.

---

## 2. Step 1: 도메인 탐구 — Subdomain 분류

- MUST: 도메인 탐구 시작 전에 `docs/ddd/context-map.md`와 기존 `docs/ddd/bounded-context-*.md §1`을 읽는다.
- MUST: 기존 BC들의 Subdomain 유형을 파악하여 일관성을 유지한다.
- MUST: 탐구 결과를 `bounded-context-{bc}.md §1 Domain`에 기록한다.

질문: "이 문제 영역이 비즈니스에서 어떤 위치를 차지하는가?"

| Subdomain 유형 | 특징 | DDD 전략 |
|--------------|------|---------|
| **Core Domain** | 경쟁 우위의 핵심. 차별화 요소 | 최고 수준 설계 투자 (Rich Domain Model, Full DDD) |
| **Supporting Domain** | 핵심을 지원. 표준화 가능 | 중간 수준 투자 (경우에 따라 CRUD) |
| **Generic Domain** | 범용 문제. 외부 솔루션 존재 | 구매/SaaS 우선 고려 |

판단 체크리스트:
- [ ] "이 도메인이 없으면 비즈니스가 성립하지 않는가?" → Core
- [ ] "경쟁사도 동일한 문제를 가지고 있으며 비슷하게 해결하는가?" → Generic
- [ ] "위 둘 다 아니면?" → Supporting

- MUST: Core Domain에는 Rich Domain Model + Full DDD를 적용한다.
- SHOULD: Supporting Domain에는 단순 CRUD 또는 Transaction Script가 적합한지 검토한다.
- SHOULD: Generic Domain은 외부 솔루션(SaaS, 오픈소스)을 먼저 평가한다.

---

## 3. Step 2: Ubiquitous Language(UL) 수립

- MUST: 도메인 전문가(PO, 기획자)와 함께 핵심 용어(명사, 동사) 목록을 만든다.
- MUST: UL 용어는 코드(클래스명, 메서드명, 변수명)에 그대로 반영한다.
- MUST NOT: 기술 용어("CRUD", "Entity", "Record")로 도메인 개념을 대체한다.
- MUST: UL 충돌 발생 시(같은 단어, 다른 의미) → Context 경계가 잘못된 신호. 경계 재검토.
- MUST: UL 결과를 `bounded-context-{bc}.md §2 Ubiquitous Language`에 기록한다.

UL 충돌 검사: 신규 용어 정의 전 `docs/ddd/bounded-context-*.md §2` 모든 파일을 읽는다.

UL 문서화 형식:
```markdown
## §2 Ubiquitous Language

| 용어(한국어) | 코드 식별자 | 정의 | 유사 용어와의 차이 |
|------------|-----------|------|-----------------|
| 사용자 | User | 서비스에 가입한 실제 사람 | Member와 다름 — Member는 팀 소속 관계 |
| 팀원 | Member | 특정 팀에 속한 User-Team 관계 | User 자체가 아님 |
```

---

## 4. Step 5: Bounded Context 확정 + Context Map 업데이트

```
[BC 간 관계 유형]

Upstream/Downstream (U/D):
  Identity BC (U) → Notification BC (D)
  Identity BC (U) → Audit BC (D)

관계 패턴:
  Shared Kernel: BC 간 공유 모델 (최소화. shared-event 모듈만 허용)
  Customer/Supplier: D가 U의 변경에 영향받음
  Conformist: D가 U의 모델을 그대로 수용
  Anti-Corruption Layer (ACL): D가 U 모델을 번역 (Integration Event 경유)
  Open Host Service (OHS): U가 표준 프로토콜로 서비스 제공
```

- MUST: 신규 BC 추가 시 기존 BC와의 관계를 Context Map에 명시한다.
- MUST: BC 간 통신은 항상 Integration Event(shared-event)를 통한다. 직접 도메인 모델 공유 금지.
- MUST: Context Map은 `docs/ddd/context-map.md`에 ASCII 다이어그램으로 유지한다.
- MUST: 신규 BC 추가 시 `docs/ddd/` 산출물을 모두 갱신한다 (`context-map-pointer.md` §3 참조).
   - `docs/ddd/context-map.md` — §1 Subdomain 표에 행 추가, §2 다이어그램에 BC + 관계 추가, §5 모듈 매핑에 Gradle 모듈 행 추가
   - `docs/ddd/bounded-context-{bc}.md` — 신규 파일 작성 (5섹션 템플릿, `bounded-context-identity.md` 참조)
   - `docs/ddd/strategic-design-changelog.md` — 변경 이력 기록

---

## 5. Step 1 & 5 참조: DDD 깊이 결정

| BC 유형 | 적용 패턴 | 판단 기준 |
|---------|----------|---------|
| Core Domain | Full DDD: Aggregate, VO, Domain Event, Domain Service, Repository Port | 복잡한 비즈니스 규칙, 상태 전이, 불변식 |
| Supporting Domain | Simplified DDD: 단순 Aggregate + CQRS Port | 규칙이 단순하거나 CRUD 중심 |
| Generic Domain | Thin Layer: Service + DTO + Repository | 외부 솔루션을 Adapter로 감쌈 |

질문 체크리스트:
- [ ] "이 BC에 복잡한 상태 전이(FSM)가 있는가?" → Full DDD
- [ ] "이 BC의 규칙이 if/else 5개 미만인가?" → Simplified 검토
- [ ] "이 BC가 외부 SaaS를 Wrapping하는가?" → Thin Layer

- SHOULD: 처음에는 Rich Domain Model로 시작하되, 단순 CRUD가 확실한 경우 Simplified로 선택한다.
- MUST NOT: 성능 이유만으로 Domain Model을 Anemic으로 만든다.

---

## 6. Step 6: ADR 작성 기준

아래 결정 중 하나 이상이 포함되면 ADR을 작성한다:
- Subdomain 분류 (Core/Supporting/Generic) 판단
- 기존 BC와 새 BC 간 경계 설정
- Context Map에서 관계 패턴 선택 (ACL, Conformist 등)
- DDD 깊이 결정 (Full vs Simplified vs Thin)

- MUST: ADR 없이 위 결정을 임의로 구현하지 않는다.
- MUST: ADR 번호는 순차 할당 (`docs/decisions/index.md` 참조).

---

## 7. Step 7: 구현 시작

전략적 설계 완료 후 `scaffold.md §Step 0 DDD 5단계 게이트`를 통과하고 `scaffold.md §Inside-Out 개발 원칙`에 따라 구현한다.

```
전략적 설계 완료 체크리스트 (scaffold.md §Step 0와 동일):
- [ ] bounded-context-{bc}.md §1 Domain 작성됨
- [ ] bounded-context-{bc}.md §2 UL 표 작성됨 (다른 BC와 용어 충돌 없음)
- [ ] bounded-context-{bc}.md §3 Entity & VO 식별됨
- [ ] bounded-context-{bc}.md §4 Aggregate & Root 설계됨
- [ ] bounded-context-{bc}.md §5 BC 경계 + Subdomain + 통합 패턴 결정됨
- [ ] context-map.md §1/§2/§5 갱신됨
- [ ] (필요 시) ADR 작성됨
     ↓
→ scaffold.md §Step 0 체크리스트 통과 → Gradle 모듈 생성 시작
```

---

## fallback 지시문

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> `docs/decisions/`에서 관련 ADR을 직접 읽어 결정 배경을 파악하라.
