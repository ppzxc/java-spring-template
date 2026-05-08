package io.github.ppzxc.boilerplate.notification.application.service;

import io.github.ppzxc.boilerplate.notification.application.dto.NotificationSummary;
import io.github.ppzxc.boilerplate.notification.application.dto.SendUserRegisteredNotificationCommand;
import io.github.ppzxc.boilerplate.notification.application.port.in.SendUserRegisteredNotificationUseCase;
import io.github.ppzxc.boilerplate.notification.application.port.out.SaveNotificationPort;
import io.github.ppzxc.boilerplate.notification.domain.Notification;
import io.github.ppzxc.boilerplate.notification.domain.NotificationChannel;
import io.github.ppzxc.boilerplate.notification.domain.NotificationContent;
import io.github.ppzxc.boilerplate.notification.domain.RecipientUserId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class SendUserRegisteredNotificationService
    implements SendUserRegisteredNotificationUseCase {

  private final SaveNotificationPort savePort;
  private final Clock clock;

  public SendUserRegisteredNotificationService(SaveNotificationPort savePort, Clock clock) {
    this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public NotificationSummary execute(SendUserRegisteredNotificationCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    var recipientUserId = new RecipientUserId(UUID.fromString(command.recipientUserId()));
    var content =
        new NotificationContent(
            command.userName() + " 님, 회원가입을 환영합니다!", command.email() + " 계정으로 등록이 완료되었습니다.");

    var notification =
        Notification.create(recipientUserId, NotificationChannel.EMAIL, content, clock.instant());

    savePort.save(notification);

    return new NotificationSummary(
        notification.id().value().toString(),
        notification.status().name(),
        notification.channel().name());
  }
}
