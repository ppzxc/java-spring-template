package io.github.ppzxc.boilerplate.identity.domain.event;

import java.time.Instant;
import java.util.UUID;

/** User Aggregate Domain Event 그룹 (D-13). */
public sealed interface UserEvent extends DomainEvent
    permits UserEvent.UserRegisteredEvent,
        UserEvent.UserSuspendedEvent,
        UserEvent.UserDeactivatedEvent {

  record UserRegisteredEvent(
      UUID eventId,
      String eventType,
      UUID aggregateId,
      Instant occurredAt,
      long aggregateVersion,
      String userName,
      String email)
      implements UserEvent {}

  record UserSuspendedEvent(
      UUID eventId, String eventType, UUID aggregateId, Instant occurredAt, long aggregateVersion)
      implements UserEvent {}

  record UserDeactivatedEvent(
      UUID eventId, String eventType, UUID aggregateId, Instant occurredAt, long aggregateVersion)
      implements UserEvent {}
}
