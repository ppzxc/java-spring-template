package io.github.ppzxc.boilerplate.notification.adapter.output.persist;

import static io.github.ppzxc.boilerplate.notification.persistence.jooq.Tables.NOTIFICATIONS;

import io.github.ppzxc.boilerplate.notification.application.port.out.LoadNotificationPort;
import io.github.ppzxc.boilerplate.notification.application.port.out.OptimisticLockException;
import io.github.ppzxc.boilerplate.notification.application.port.out.SaveNotificationPort;
import io.github.ppzxc.boilerplate.notification.domain.Notification;
import io.github.ppzxc.boilerplate.notification.domain.NotificationId;
import java.util.Objects;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort {

  private final DSLContext dsl;
  private final ApplicationEventPublisher eventPublisher;

  public NotificationPersistenceAdapter(DSLContext dsl, ApplicationEventPublisher eventPublisher) {
    this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
  }

  @Override
  public Optional<Notification> findById(NotificationId id) {
    return dsl.selectFrom(NOTIFICATIONS)
        .where(NOTIFICATIONS.ID.eq(id.value()))
        .fetchOptional(NotificationPersistenceMapper::toDomain);
  }

  @Override
  public void save(Notification entity) {
    if (entity.version() == 0) {
      // 신규 알림 INSERT (AD-7: version=0이면 INSERT)
      dsl.insertInto(NOTIFICATIONS)
          .set(NOTIFICATIONS.ID, entity.id().value())
          .set(NOTIFICATIONS.RECIPIENT_ID, entity.recipientUserId().value())
          .set(NOTIFICATIONS.CHANNEL, entity.channel().name())
          .set(NOTIFICATIONS.STATUS, entity.status().name())
          .set(NOTIFICATIONS.SUBJECT, entity.content().subject())
          .set(NOTIFICATIONS.BODY, entity.content().body())
          .set(NOTIFICATIONS.VERSION, entity.version())
          .execute();
    } else {
      // 상태 변경 UPDATE (AD-7: WHERE version = ? — Optimistic Lock)
      int affected =
          dsl.update(NOTIFICATIONS)
              .set(NOTIFICATIONS.STATUS, entity.status().name())
              .set(NOTIFICATIONS.VERSION, entity.version() + 1)
              .where(NOTIFICATIONS.ID.eq(entity.id().value()))
              .and(NOTIFICATIONS.VERSION.eq(entity.version()))
              .execute();
      if (affected == 0) {
        throw new OptimisticLockException(entity.id());
      }
    }

    // 이벤트 수거 후 Outbox 발행 (AD-3: 동일 TX)
    entity.pullDomainEvents().forEach(eventPublisher::publishEvent);
  }
}
