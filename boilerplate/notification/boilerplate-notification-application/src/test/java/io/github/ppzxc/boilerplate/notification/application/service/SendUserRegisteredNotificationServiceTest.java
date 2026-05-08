package io.github.ppzxc.boilerplate.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.ppzxc.boilerplate.notification.application.dto.NotificationSummary;
import io.github.ppzxc.boilerplate.notification.application.dto.SendUserRegisteredNotificationCommand;
import io.github.ppzxc.boilerplate.notification.application.port.out.SaveNotificationPort;
import io.github.ppzxc.boilerplate.notification.domain.Notification;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendUserRegisteredNotificationServiceTest {

  private final SaveNotificationPort savePort = mock(SaveNotificationPort.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private final SendUserRegisteredNotificationService sut =
      new SendUserRegisteredNotificationService(savePort, clock);

  private static SendUserRegisteredNotificationCommand validCommand() {
    return new SendUserRegisteredNotificationCommand(
        UUID.randomUUID().toString(),
        "홍길동",
        "test@example.com",
        Instant.parse("2026-01-01T00:00:00Z").toString());
  }

  @Test
  void 정상_알림_저장() {
    NotificationSummary result = sut.execute(validCommand());

    verify(savePort).save(any(Notification.class));
    assertThat(result.notificationId()).isNotNull();
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.channel()).isEqualTo("EMAIL");
  }

  @Test
  void 저장된_알림은_PENDING_상태() {
    NotificationSummary result = sut.execute(validCommand());

    assertThat(result.status()).isEqualTo("PENDING");
  }

  @Test
  void null_savePort_생성_실패() {
    assertThatThrownBy(() -> new SendUserRegisteredNotificationService(null, clock))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void null_clock_생성_실패() {
    assertThatThrownBy(() -> new SendUserRegisteredNotificationService(savePort, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void null_command_실패() {
    assertThatThrownBy(() -> sut.execute(null)).isInstanceOf(NullPointerException.class);

    verify(savePort, never()).save(any());
  }

  @Test
  void command_self_validation_null_recipientUserId() {
    assertThatThrownBy(
            () ->
                new SendUserRegisteredNotificationCommand(
                    null, "홍길동", "test@example.com", "2026-01-01T00:00:00Z"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void command_self_validation_null_userName() {
    assertThatThrownBy(
            () ->
                new SendUserRegisteredNotificationCommand(
                    UUID.randomUUID().toString(), null, "test@example.com", "2026-01-01T00:00:00Z"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void command_self_validation_null_email() {
    assertThatThrownBy(
            () ->
                new SendUserRegisteredNotificationCommand(
                    UUID.randomUUID().toString(), "홍길동", null, "2026-01-01T00:00:00Z"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
