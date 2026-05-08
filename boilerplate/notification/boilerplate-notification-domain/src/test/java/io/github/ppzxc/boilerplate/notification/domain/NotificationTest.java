package io.github.ppzxc.boilerplate.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ppzxc.boilerplate.test.DomainTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest extends DomainTestBase {

  private static final UUID RECIPIENT_UUID = fixedUuidV7(1_700_000_000_000L);

  @Test
  void create_정상_PENDING_상태() {
    var notification =
        Notification.create(
            new RecipientUserId(RECIPIENT_UUID),
            NotificationChannel.EMAIL,
            new NotificationContent("환영합니다", "가입을 축하드립니다."),
            NOW);

    assertThat(notification.id()).isNotNull();
    assertThat(notification.recipientUserId()).isEqualTo(new RecipientUserId(RECIPIENT_UUID));
    assertThat(notification.channel()).isEqualTo(NotificationChannel.EMAIL);
    assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
    assertThat(notification.content()).isEqualTo(new NotificationContent("환영합니다", "가입을 축하드립니다."));
    assertThat(notification.version()).isEqualTo(0L);
  }

  @Test
  void create_append_only_이벤트_없음() {
    var notification =
        Notification.create(
            new RecipientUserId(RECIPIENT_UUID),
            NotificationChannel.EMAIL,
            new NotificationContent("제목", "본문"),
            NOW);

    assertThat(notification.pullDomainEvents()).isEmpty();
  }

  @Test
  void create_null_recipientUserId_실패() {
    assertThatThrownBy(
            () ->
                Notification.create(
                    null, NotificationChannel.EMAIL, new NotificationContent("제목", "본문"), NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_null_channel_실패() {
    assertThatThrownBy(
            () ->
                Notification.create(
                    new RecipientUserId(RECIPIENT_UUID),
                    null,
                    new NotificationContent("제목", "본문"),
                    NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_null_content_실패() {
    assertThatThrownBy(
            () ->
                Notification.create(
                    new RecipientUserId(RECIPIENT_UUID), NotificationChannel.EMAIL, null, NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_null_occurredAt_실패() {
    assertThatThrownBy(
            () ->
                Notification.create(
                    new RecipientUserId(RECIPIENT_UUID),
                    NotificationChannel.EMAIL,
                    new NotificationContent("제목", "본문"),
                    null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void reconstitute_이벤트_없음() {
    var id = new NotificationId(fixedUuidV7(1_700_000_000_000L));
    var notification =
        Notification.reconstitute(
            id,
            new RecipientUserId(RECIPIENT_UUID),
            NotificationChannel.EMAIL,
            NotificationStatus.PENDING,
            new NotificationContent("제목", "본문"),
            0L);

    assertThat(notification.id()).isEqualTo(id);
    assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
    assertThat(notification.pullDomainEvents()).isEmpty();
  }
}
