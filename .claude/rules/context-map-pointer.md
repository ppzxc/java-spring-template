# Context Map Pointer

BC 작업 시 전략적 설계 산출물 참조 강제 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.
>
> 본 파일은 "포인터"로 동작한다. 실제 BC 구조 정보는 `docs/ddd/`에 있다.
> 본 파일은 어떤 산출물을 언제 읽고 갱신해야 하는지의 워크플로우만 규정한다.

---

## 1. BC 작업 시 필수 참조

MUST: 다음 작업을 시작하기 전에 아래 산출물을 모두 읽는다.

- 신규 BC 추가
- 기존 BC의 Aggregate Root, VO, Domain Event 추가/변경
- BC 간 통합 패턴 관련 작업 (Integration Event 추가/변경)
- Gradle 모듈 추가/이동

| 산출물 파일 | 역할 |
|---|---|
| `docs/ddd/context-map.md` | 전체 BC 관계 + Subdomain 분류 + 모듈 매핑(§5) |
| `docs/ddd/bounded-context-{bc}.md` | 작업 BC의 UL + Domain/Entity·VO/Aggregate/BC 경계 전체 |

---

## 2. DDD 5단계별 산출물 매핑

| DDD 단계 | 산출물 위치 | 갱신 시점 |
|---|---|---|
| 1. 도메인 탐구 | `bounded-context-{bc}.md` §1 Domain | 비즈니스 범위 변경 시 |
| 2. UL 수립 | `bounded-context-{bc}.md` §2 Ubiquitous Language | UL 용어 추가/변경 시 |
| 3. Entity & VO | `bounded-context-{bc}.md` §3 + `domain/model/` | VO/Entity 추가 시 |
| 4. Aggregate & Root | `bounded-context-{bc}.md` §4 + `domain/model/` | Aggregate 추가/변경 시 |
| 5. BC 경계 + Subdomain | `bounded-context-{bc}.md` §5 + `context-map.md` §1, §2, §5 | BC 추가/Subdomain 재분류 시 |

---

## 3. UL 충돌 검사 (신규 BC 또는 신규 용어 추가 시)

MUST: 신규 UL 용어를 정의하기 전에 `docs/ddd/bounded-context-*.md` 모든 파일의 §2를 읽는다.

MUST: 같은 용어가 다른 BC에서 다른 의미로 정의되어 있으면 다음 중 하나를 수행한다.
1. 신규 BC 용어를 변경한다 (예: `User` → `Account`, `Member` → `Subscriber`)
2. BC 경계가 잘못되었음을 사용자에게 보고하고 경계 재검토를 제안한다

---

## 4. 작업 완료 후 산출물 갱신 (MUST)

| 작업 유형 | 갱신 대상 |
|---|---|
| 신규 BC 추가 | `context-map.md`(§1 Subdomain 표, §2 다이어그램, §5 모듈 매핑), `bounded-context-{bc}.md`(신규 작성, 5섹션), `strategic-design-changelog.md` |
| 신규 Aggregate/VO 추가 | `bounded-context-{bc}.md` §3/§4, `strategic-design-changelog.md` |
| BC 간 Integration Event 추가/변경 | `context-map.md`(§2 다이어그램), `bounded-context-{bc}.md` §5, `strategic-design-changelog.md` |
| Gradle 모듈 추가/이동 | `context-map.md` §5, `strategic-design-changelog.md` |

> ADR 작성 여부는 본 파일이 아닌 `strategic-design.md` §6 기준에 따른다.
> ADR이 작성된 경우에만 `strategic-design-changelog.md`의 ADR 컬럼에 번호를 기재한다.

---

## fallback 지시문

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> `docs/ddd/context-map.md`를 먼저 읽어 전체 BC 구조를 파악한 후 최적의 대안을 제안하라.
