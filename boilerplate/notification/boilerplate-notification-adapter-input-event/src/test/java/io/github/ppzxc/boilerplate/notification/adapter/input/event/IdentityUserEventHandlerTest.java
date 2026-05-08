package io.github.ppzxc.boilerplate.notification.adapter.input.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.ppzxc.boilerplate.notification.application.dto.NotificationSummary;
import io.github.ppzxc.boilerplate.notification.application.dto.SendUserRegisteredNotificationCommand;
import io.github.ppzxc.boilerplate.notification.application.port.in.SendUserRegisteredNotificationUseCase;
import io.github.ppzxc.boilerplate.shared.UserRegisteredIntegrationEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
class IdentityUserEventHandlerTest {

  @MockitoBean SendUserRegisteredNotificationUseCase useCase;

  @Test
  void UserRegisteredIntegrationEvent_수신_시_UseCase_호출(Scenario scenario) throws Exception {
    var captured = new AtomicReference<SendUserRegisteredNotificationCommand>();
    when(useCase.execute(any()))
        .thenAnswer(
            inv -> {
              captured.set(inv.getArgument(0));
              return new NotificationSummary(UUID.randomUUID().toString(), "PENDING", "EMAIL");
            });

    var userId = UUID.randomUUID();
    var occurredAt = Instant.parse("2026-01-01T00:00:00Z");
    var event = new UserRegisteredIntegrationEvent(userId, "홍길동", "test@example.com", occurredAt);

    scenario
        .publish(event)
        .andWaitForStateChange(() -> captured.get())
        .andVerify(
            cmd -> {
              assertThat(cmd.recipientUserId()).isEqualTo(userId.toString());
              assertThat(cmd.userName()).isEqualTo("홍길동");
              assertThat(cmd.email()).isEqualTo("test@example.com");
              assertThat(cmd.occurredAt()).isEqualTo(occurredAt.toString());
            });
  }
}
