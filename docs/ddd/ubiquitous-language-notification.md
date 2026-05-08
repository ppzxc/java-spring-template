# Ubiquitous Language — Notification BC

> 도메인 전문가와 개발자가 공유하는 Notification BC의 용어 사전.
> 코드 식별자(클래스명, 메서드명)에 그대로 반영한다.
>
> 신규 용어 추가/변경 시 `strategic-design-changelog.md`에 기록한다.
> UL 충돌 검사: 이 파일의 용어와 다른 BC 용어 간 충돌 여부를 확인한다.

---

| 용어(한국어) | 코드 식별자 | 정의 | 유사 용어와의 차이 |
|------------|-----------|------|-----------------|
| 알림 | `Notification` | 특정 수신자에게 발송 예정인 메시지 단위 (채널, 상태, 내용 포함) | Identity BC의 `User`와 다름 — Notification은 발송 행위와 그 결과를 표현 |
| 알림 ID | `NotificationId` | UUIDv7 식별자 VO | 단순 `UUID`와 구분 — VO 래핑, 타입 안전성 보장 |
| 수신자 사용자 ID | `RecipientUserId` | 알림을 수신할 대상의 사용자 ID VO | Identity BC의 `UserId`와 의미는 같으나 Notification BC의 경계 내 자체 VO로 정의 (ACL 패턴) |
| 알림 채널 | `NotificationChannel` | 발송 수단 enum: `EMAIL`, `SMS`, `PUSH` | 발송 채널을 코드로 고정 — 동적 확장 시 enum 항목 추가 |
| 알림 상태 | `NotificationStatus` | `PENDING`(발송 대기) / `SENT`(발송 완료) / `FAILED`(발송 실패) enum | `PENDING`이 초기 상태 — Phase 5에서는 INSERT만 발생하므로 상태 전이 메서드는 향후 구현 |
| 알림 내용 | `NotificationContent` | subject(제목, 최대 200자) + body(본문, 최대 5000자) VO | 단순 `String` 아님 — 길이 제약 포함한 자기검증 VO |
| 알림 생성 이벤트 | `NotificationCreatedEvent` | Notification Aggregate 생성 시 발행되는 Domain Event | 현재 BC 내부에서만 사용 — Integration Event 아님 |
| 회원가입 알림 발송 유스케이스 | `SendUserRegisteredNotificationUseCase` | `UserRegisteredIntegrationEvent` 수신 시 PENDING 상태 알림을 notifications 테이블에 적재하는 유스케이스 | 실제 외부 발송(SMTP 등)은 별도 UseCase로 확장 예정 |
