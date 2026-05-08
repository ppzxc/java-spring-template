package io.github.ppzxc.boilerplate.identity.application.dto;

import java.util.Objects;

public record FindUserByIdQuery(String userId) {

  public FindUserByIdQuery {
    Objects.requireNonNull(userId, "userId must not be null");
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
  }
}
