package io.github.ppzxc.boilerplate.identity.domain.model;

import io.github.ppzxc.boilerplate.identity.domain.event.DomainEvent;
import io.github.ppzxc.boilerplate.identity.domain.event.UserEvent;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class User {

  private final UserId id;
  private final Email email;
  private final UserName userName;
  private Credential credential;
  private UserStatus status;
  private final Instant createdAt;
  private Instant updatedAt;
  private final long version;
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  private User(
      UserId id,
      Email email,
      UserName userName,
      Credential credential,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.email = Objects.requireNonNull(email, "email must not be null");
    this.userName = Objects.requireNonNull(userName, "userName must not be null");
    this.credential = Objects.requireNonNull(credential, "credential must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    this.version = version;
  }

  public static User create(UserName userName, Email email, HashedPassword password, Instant now) {
    Objects.requireNonNull(userName, "userName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(password, "password must not be null");
    Objects.requireNonNull(now, "now must not be null");
    var id = UserId.generate();
    var credential = Credential.create(password, now);
    var user = new User(id, email, userName, credential, UserStatus.ACTIVE, now, now, 0L);
    user.registerEvent(
        new UserEvent.UserRegisteredEvent(
            UUIDv7.generate(),
            "UserRegisteredEvent",
            id.value(),
            now,
            0L,
            userName.value(),
            email.value()));
    return user;
  }

  public static User reconstitute(
      UserId id,
      Email email,
      UserName userName,
      HashedPassword password,
      Instant credentialCreatedAt,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    var credential = Credential.reconstitute(password, credentialCreatedAt);
    return new User(id, email, userName, credential, status, createdAt, updatedAt, version);
  }

  public void assertCanLogin() {
    if (this.status != UserStatus.ACTIVE) {
      throw new UserException.IneligibleStatusException(this.status.name());
    }
  }

  public void suspend(Instant now) {
    Objects.requireNonNull(now, "now must not be null");
    if (this.status == UserStatus.SUSPENDED) {
      throw new UserException.AlreadySuspendedException(this.id.value().toString());
    }
    if (this.status == UserStatus.DEACTIVATED) {
      throw new UserException.AlreadyDeactivatedException(this.id.value().toString());
    }
    this.status = UserStatus.SUSPENDED;
    this.updatedAt = now;
    registerEvent(
        new UserEvent.UserSuspendedEvent(
            UUIDv7.generate(), "UserSuspendedEvent", id.value(), now, version));
  }

  public void deactivate(Instant now) {
    Objects.requireNonNull(now, "now must not be null");
    if (this.status == UserStatus.DEACTIVATED) {
      throw new UserException.AlreadyDeactivatedException(this.id.value().toString());
    }
    this.status = UserStatus.DEACTIVATED;
    this.updatedAt = now;
    registerEvent(
        new UserEvent.UserDeactivatedEvent(
            UUIDv7.generate(), "UserDeactivatedEvent", id.value(), now, version));
  }

  private void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  public List<DomainEvent> pullDomainEvents() {
    var events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }

  public UserId id() {
    return id;
  }

  public Email email() {
    return email;
  }

  public UserName userName() {
    return userName;
  }

  public HashedPassword hashedPassword() {
    return credential.hashedPassword();
  }

  public Instant credentialCreatedAt() {
    return credential.createdAt();
  }

  public UserStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }
}
