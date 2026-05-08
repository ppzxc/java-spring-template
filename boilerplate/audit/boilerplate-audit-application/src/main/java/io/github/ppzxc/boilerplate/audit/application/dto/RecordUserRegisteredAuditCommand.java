package io.github.ppzxc.boilerplate.audit.application.dto;

import java.util.Objects;

/** UserRegisteredIntegrationEvent를 수신하여 감사 로그 적재를 명령하는 Command. */
public record RecordUserRegisteredAuditCommand(
    String subjectUserId, String userName, String email, String occurredAt) {

  public RecordUserRegisteredAuditCommand {
    Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
    Objects.requireNonNull(userName, "userName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    if (subjectUserId.isBlank()) {
      throw new IllegalArgumentException("subjectUserId must not be blank");
    }
    if (userName.isBlank()) {
      throw new IllegalArgumentException("userName must not be blank");
    }
    if (email.isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }
    if (occurredAt.isBlank()) {
      throw new IllegalArgumentException("occurredAt must not be blank");
    }
  }
}
