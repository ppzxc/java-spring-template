package io.github.ppzxc.boilerplate.identity.adapter.output.persist;

import static io.github.ppzxc.boilerplate.identity.persistence.jooq.Tables.USERS;

import io.github.ppzxc.boilerplate.identity.application.port.out.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.out.OptimisticLockException;
import io.github.ppzxc.boilerplate.identity.application.port.out.SaveUserPort;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

  private final DSLContext dsl;
  private final ApplicationEventPublisher eventPublisher;

  public UserPersistenceAdapter(DSLContext dsl, ApplicationEventPublisher eventPublisher) {
    this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
  }

  @Override
  public Optional<User> findById(UserId id) {
    return dsl.selectFrom(USERS)
        .where(USERS.ID.eq(id.value()))
        .fetchOptional(UserPersistenceMapper::toDomain);
  }

  @Override
  public boolean existsByEmail(Email email) {
    return dsl.fetchExists(USERS, USERS.EMAIL.eq(email.value()));
  }

  @Override
  public User save(User entity) {
    // UPDATE WHERE version = ? (Optimistic Lock — AD-7)
    int affected =
        dsl.update(USERS)
            .set(USERS.EMAIL, entity.email().value())
            .set(USERS.USER_NAME, entity.userName().value())
            .set(USERS.HASHED_PASSWORD, entity.hashedPassword().value())
            .set(USERS.CRED_CREATED_AT, entity.credentialCreatedAt().atOffset(ZoneOffset.UTC))
            .set(USERS.STATUS, entity.status().name())
            .set(USERS.VERSION, entity.version() + 1)
            .set(USERS.UPDATED_AT, entity.updatedAt().atOffset(ZoneOffset.UTC))
            .where(USERS.ID.eq(entity.id().value()))
            .and(USERS.VERSION.eq(entity.version()))
            .execute();

    if (affected == 0) {
      boolean exists = dsl.fetchExists(USERS, USERS.ID.eq(entity.id().value()));
      if (exists) {
        throw new OptimisticLockException(entity.id().value().toString());
      }
      // 신규 엔티티: INSERT
      dsl.insertInto(USERS)
          .set(USERS.ID, entity.id().value())
          .set(USERS.EMAIL, entity.email().value())
          .set(USERS.USER_NAME, entity.userName().value())
          .set(USERS.HASHED_PASSWORD, entity.hashedPassword().value())
          .set(USERS.CRED_CREATED_AT, entity.credentialCreatedAt().atOffset(ZoneOffset.UTC))
          .set(USERS.STATUS, entity.status().name())
          .set(USERS.VERSION, entity.version())
          .set(USERS.CREATED_AT, entity.createdAt().atOffset(ZoneOffset.UTC))
          .set(USERS.UPDATED_AT, entity.updatedAt().atOffset(ZoneOffset.UTC))
          .execute();
    }

    // 이벤트 수거 후 Outbox 발행 (AD-3: 동일 TX)
    entity.pullDomainEvents().forEach(eventPublisher::publishEvent);
    return entity;
  }
}
