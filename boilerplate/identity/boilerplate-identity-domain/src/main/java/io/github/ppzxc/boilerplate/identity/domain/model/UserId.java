package io.github.ppzxc.boilerplate.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "UserId must not be null");
  }

  public static UserId generate() {
    return new UserId(UUIDv7.generate());
  }
}
