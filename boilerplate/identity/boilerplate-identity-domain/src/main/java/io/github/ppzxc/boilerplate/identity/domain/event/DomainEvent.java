package io.github.ppzxc.boilerplate.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** BC 내 Domain Event 표준 인터페이스 (5필드 필수, domain.md §13). */
public interface DomainEvent {

  UUID eventId();

  String eventType();

  UUID aggregateId();

  Instant occurredAt();

  long aggregateVersion();
}
