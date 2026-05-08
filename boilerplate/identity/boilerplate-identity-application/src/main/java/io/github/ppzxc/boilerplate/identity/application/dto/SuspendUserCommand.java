package io.github.ppzxc.boilerplate.identity.application.dto;

import java.util.Objects;

public record SuspendUserCommand(String userId) {

  public SuspendUserCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
  }
}
