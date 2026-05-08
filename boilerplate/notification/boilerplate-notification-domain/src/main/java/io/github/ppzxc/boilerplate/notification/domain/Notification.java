package io.github.ppzxc.boilerplate.notification.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Notification Aggregate Root.
 *
 * <p>append-only 변형 (domain.md §9): 현재 컨슈머 없음 → create()에서 이벤트 미발행.
 */
public final class Notification {

  private final NotificationId id;
  private final RecipientUserId recipientUserId;
  private final NotificationChannel channel;
  private NotificationStatus status;
  private final NotificationContent content;
  private final long version;
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  private Notification(
      NotificationId id,
      RecipientUserId recipientUserId,
      NotificationChannel channel,
      NotificationStatus status,
      NotificationContent content,
      long version) {
    this.id = Objects.requireNonNull(id, "NotificationId must not be null");
    this.recipientUserId =
        Objects.requireNonNull(recipientUserId, "RecipientUserId must not be null");
    this.channel = Objects.requireNonNull(channel, "NotificationChannel must not be null");
    this.status = Objects.requireNonNull(status, "NotificationStatus must not be null");
    this.content = Objects.requireNonNull(content, "NotificationContent must not be null");
    this.version = version;
  }

  public static Notification create(
      RecipientUserId recipientUserId,
      NotificationChannel channel,
      NotificationContent content,
      Instant occurredAt) {
    Objects.requireNonNull(recipientUserId, "RecipientUserId must not be null");
    Objects.requireNonNull(channel, "NotificationChannel must not be null");
    Objects.requireNonNull(content, "NotificationContent must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    return new Notification(
        NotificationId.generate(),
        recipientUserId,
        channel,
        NotificationStatus.PENDING,
        content,
        0L);
  }

  public static Notification reconstitute(
      NotificationId id,
      RecipientUserId recipientUserId,
      NotificationChannel channel,
      NotificationStatus status,
      NotificationContent content,
      long version) {
    return new Notification(id, recipientUserId, channel, status, content, version);
  }

  public List<DomainEvent> pullDomainEvents() {
    var events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }

  public NotificationId id() {
    return id;
  }

  public RecipientUserId recipientUserId() {
    return recipientUserId;
  }

  public NotificationChannel channel() {
    return channel;
  }

  public NotificationStatus status() {
    return status;
  }

  public NotificationContent content() {
    return content;
  }

  public long version() {
    return version;
  }
}
