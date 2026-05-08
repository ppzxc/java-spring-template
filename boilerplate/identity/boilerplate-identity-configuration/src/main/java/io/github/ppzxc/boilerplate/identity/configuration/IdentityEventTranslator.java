package io.github.ppzxc.boilerplate.identity.configuration;

import io.github.ppzxc.boilerplate.identity.domain.event.UserEvent.UserRegisteredEvent;
import io.github.ppzxc.boilerplate.shared.UserRegisteredIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class IdentityEventTranslator {

  private final ApplicationEventPublisher publisher;

  IdentityEventTranslator(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  // REQUIRES_NEW: 통합 이벤트를 트랜잭션 내에서 발행해 @ApplicationModuleListener가 AFTER_COMMIT에 트리거되도록 보장
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void on(UserRegisteredEvent event) {
    publisher.publishEvent(
        new UserRegisteredIntegrationEvent(
            event.aggregateId(), event.userName(), event.email(), event.occurredAt()));
  }

  // --- AI_ANCHOR: ADD_NEW_EVENT_TRANSLATION_HERE ---
}
