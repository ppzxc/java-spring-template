---
description: 클래스/패키지/모듈/파일 네이밍 규칙, 금지 접미사 (D-12)
alwaysApply: true
---

# 네이밍 규칙

## 전체 네이밍 테이블

| 계층 | 객체 구분 | 명명 규칙 | 예시 |
|------|-----------|-----------|------|
| Adapter (In) | Request DTO | `{Subject}Request` | `UserRegistrationRequest` |
| Adapter (In) | Response DTO | `{Subject}Response` | `UserDetailResponse` |
| Core (In) | Command | `{Verb}{Subject}Command` | `RegisterUserCommand` |
| Core (In) | Query | `{Verb}{Subject}Query` | `FindUserByIdQuery` |
| Core (In) | Command UseCase | `{Verb}{Subject}UseCase` | `RegisterUserUseCase` (인터페이스) |
| Core (In) | Query UseCase | `{Verb}{Subject}UseCase` | `FindUserByIdUseCase` (인터페이스) |
| Core | UseCase 구현체 | `{Verb}{Subject}Service` | `RegisterUserService` |
| Core | Domain Service | `{Subject}DomainService` | `UserDomainService` |
| Core (Domain) | Aggregate Root | `{Subject}` | `User` |
| Core (Domain) | Value Object | `{Subject}` | `Email`, `UserName` |
| Core (Domain) | Domain Event | `{AggregateRoot}{PastTenseVerb}Event` | `UserRegisteredEvent` |
| Core (Out) | Load Port | `Load{Subject}Port` | `LoadUserPort` |
| Core (Out) | Save Port | `Save{Subject}Port` | `SaveUserPort` |
| Core (Out) | Query Port | `{Subject}QueryPort` | `UserQueryPort` |
| Core (Out) | EventPublisher Port | `{BC}EventPublisherPort` | `IdentityEventPublisherPort` |
| Adapter (Out) | EventPublisher 구현체 | `{Infra}{BC}EventPublisher` | `KafkaIdentityEventPublisher`, `WebhookIdentityEventPublisher` |
| Adapter (Out) | Persistence 구현체 | `{Subject}PersistenceAdapter` | `UserPersistenceAdapter` |
| Adapter (Out) | Query 구현체 | `{Subject}QueryAdapter` | `UserQueryAdapter` |
| Adapter (Out) | Cache 구현체 | `{Infra}{Subject}Cache` | `RedisUserCache` |

## Infra/Mechanism 접두어

외부 기술을 명시해야 하는 클래스에 접두어를 붙인다:

| 접두어 | 용도 |
|--------|------|
| `Rdb` | RDBMS 전반 |
| `Redis` | Redis 캐시 |
| `Http` | 외부 HTTP API 호출 |
| `Grpc` | gRPC 클라이언트 |
| `InMemory` | 테스트용 인메모리 구현 |
| `Webhook` | 이벤트 외부 전파 (Webhook) |
| `Mq` | 메시지 큐 연동 |

## 금지 접미사 (D-12)

MUST NOT: 아래 접미사를 클래스명에 사용하지 않는다.

| 금지 접미사 | 대안 |
|---|---|
| `*Handler` | 행위를 표현하는 Domain 메서드 또는 UseCase |
| `*Processor` | Domain Service 또는 UseCase |
| `*Manager` | 구체적 도메인 역할명 |
| `*Helper` | VO의 행위 메서드 |
| `*Util` | VO의 행위 메서드 |
| `*VO` | 도메인 개념 그 자체를 이름으로 (예: `Email`, not `EmailVO`) |
| `*Entity` | 도메인 개념 그 자체를 이름으로 (예: `User`, not `UserEntity`) |

## 모듈 네이밍

MUST: 모든 모듈은 프로젝트명을 접두어로 공유한다.

패턴: `{project}-{bc}-{layer}`

계층(layer) 키워드:
- `domain`
- `application`
- `adapter-input-api`
- `adapter-input-event`
- `adapter-output-persist`
- `adapter-output-cache`
- `adapter-output-http`
- `configuration`

BC에 속하지 않는 공유 모듈:
- `{project}-boot` — 전체 조립, 단일 배포 JAR (Spring Boot 진입점)
- `{project}-shared-event` — Published Language (BC 간 이벤트 계약)

## DDL 파일 네이밍

MUST: `V{n}__create_{subject_snake_case}.sql`

예: `V1__create_user.sql`, `V2__create_order.sql`

---

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 명시된 ADR 번호에 해당하는 `docs/decisions/` 파일을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
