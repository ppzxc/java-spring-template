package io.github.ppzxc.boilerplate.identity.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

  private static final Pattern RFC5322 = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  public Email {
    Objects.requireNonNull(value, "Email must not be null");
    if (!RFC5322.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email format: " + value);
    }
  }
}
