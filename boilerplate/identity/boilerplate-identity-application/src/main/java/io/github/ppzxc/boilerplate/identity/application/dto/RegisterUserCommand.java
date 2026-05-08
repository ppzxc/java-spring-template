package io.github.ppzxc.boilerplate.identity.application.dto;

import java.util.Objects;

public record RegisterUserCommand(String userName, String email, String hashedPassword) {

  public RegisterUserCommand {
    Objects.requireNonNull(userName, "userName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(hashedPassword, "hashedPassword must not be null");
    if (userName.isBlank()) {
      throw new IllegalArgumentException("userName must not be blank");
    }
    if (email.isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }
    if (hashedPassword.isBlank()) {
      throw new IllegalArgumentException("hashedPassword must not be blank");
    }
  }
}
