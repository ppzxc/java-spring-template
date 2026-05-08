package io.github.ppzxc.boilerplate.notification.adapter.input.event;

import io.github.ppzxc.boilerplate.notification.application.dto.SendUserRegisteredNotificationCommand;
import io.github.ppzxc.boilerplate.notification.application.port.in.SendUserRegisteredNotificationUseCase;
import io.github.ppzxc.boilerplate.shared.UserRegisteredIntegrationEvent;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class IdentityUserEventHandler {

  private final SendUserRegisteredNotificationUseCase useCase;

  public IdentityUserEventHandler(SendUserRegisteredNotificationUseCase useCase) {
    this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
  }

  @ApplicationModuleListener
  void on(UserRegisteredIntegrationEvent event) {
    useCase.execute(
        new SendUserRegisteredNotificationCommand(
            event.userId().toString(),
            event.userName(),
            event.email(),
            event.occurredAt().toString()));
  }
}
