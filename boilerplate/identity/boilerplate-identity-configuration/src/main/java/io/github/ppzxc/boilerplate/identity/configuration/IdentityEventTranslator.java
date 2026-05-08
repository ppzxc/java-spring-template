package io.github.ppzxc.boilerplate.identity.configuration;

import io.github.ppzxc.boilerplate.identity.domain.event.UserEvent.UserRegisteredEvent;
import io.github.ppzxc.boilerplate.shared.UserRegisteredIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class IdentityEventTranslator {

  private final ApplicationEventPublisher publisher;

  IdentityEventTranslator(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @TransactionalEventListener
  void on(UserRegisteredEvent event) {
    publisher.publishEvent(
        new UserRegisteredIntegrationEvent(
            event.aggregateId(), event.userName(), event.email(), event.occurredAt()));
  }

  // --- AI_ANCHOR: ADD_NEW_EVENT_TRANSLATION_HERE ---
}
