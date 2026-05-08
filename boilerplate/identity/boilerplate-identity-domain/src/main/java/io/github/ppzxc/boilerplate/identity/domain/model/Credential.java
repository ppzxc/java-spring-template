package io.github.ppzxc.boilerplate.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

/** User Aggregate 내부 Entity — 외부에서 직접 생성 금지. */
public final class Credential {

  private final HashedPassword hashedPassword;
  private final Instant createdAt;

  private Credential(HashedPassword hashedPassword, Instant createdAt) {
    this.hashedPassword = Objects.requireNonNull(hashedPassword, "hashedPassword must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  static Credential create(HashedPassword hashedPassword, Instant now) {
    return new Credential(hashedPassword, now);
  }

  static Credential reconstitute(HashedPassword hashedPassword, Instant createdAt) {
    return new Credential(hashedPassword, createdAt);
  }

  public HashedPassword hashedPassword() {
    return hashedPassword;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
