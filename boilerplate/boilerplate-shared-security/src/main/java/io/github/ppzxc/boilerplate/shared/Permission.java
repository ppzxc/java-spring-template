package io.github.ppzxc.boilerplate.shared;

import java.util.Objects;

public record Permission(String value) {

  public Permission {
    Objects.requireNonNull(value, "Permission value must not be null");
    if (!value.matches("[a-z]+:[a-z]+")) {
      throw new IllegalArgumentException("Permission must be resource:scope format: " + value);
    }
  }

  public String resource() {
    return value.substring(0, value.indexOf(':'));
  }

  public String scope() {
    return value.substring(value.indexOf(':') + 1);
  }
}
