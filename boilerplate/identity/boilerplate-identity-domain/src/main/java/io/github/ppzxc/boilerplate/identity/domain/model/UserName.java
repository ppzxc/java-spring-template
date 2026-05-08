package io.github.ppzxc.boilerplate.identity.domain.model;

import java.util.Objects;

public record UserName(String value) {

  public UserName {
    Objects.requireNonNull(value, "UserName must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("UserName must not be blank");
    }
    if (value.length() > 50) {
      throw new IllegalArgumentException("UserName must not exceed 50 characters");
    }
  }
}
