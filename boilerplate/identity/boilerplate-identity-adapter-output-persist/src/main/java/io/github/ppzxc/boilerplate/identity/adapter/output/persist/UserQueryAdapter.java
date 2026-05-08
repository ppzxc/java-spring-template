package io.github.ppzxc.boilerplate.identity.adapter.output.persist;

import static io.github.ppzxc.boilerplate.identity.persistence.jooq.Tables.USERS;

import io.github.ppzxc.boilerplate.identity.application.dto.UserSummary;
import io.github.ppzxc.boilerplate.identity.application.port.out.UserQueryPort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class UserQueryAdapter implements UserQueryPort {

  private final DSLContext dsl;

  public UserQueryAdapter(DSLContext dsl) {
    this.dsl = Objects.requireNonNull(dsl, "dsl must not be null");
  }

  @Override
  public Optional<UserSummary> findSummaryById(String userId) {
    return dsl.select(
            USERS.ID,
            USERS.USER_NAME,
            USERS.EMAIL,
            USERS.STATUS,
            USERS.VERSION,
            USERS.CREATED_AT,
            USERS.UPDATED_AT)
        .from(USERS)
        .where(USERS.ID.eq(UUID.fromString(userId)))
        .fetchOptional(
            r ->
                new UserSummary(
                    r.get(USERS.ID).toString(),
                    r.get(USERS.USER_NAME),
                    r.get(USERS.EMAIL),
                    r.get(USERS.STATUS),
                    r.get(USERS.VERSION),
                    r.get(USERS.CREATED_AT).toInstant(),
                    r.get(USERS.UPDATED_AT).toInstant()));
  }

  @Override
  public List<UserSummary> findAll() {
    return dsl.select(
            USERS.ID,
            USERS.USER_NAME,
            USERS.EMAIL,
            USERS.STATUS,
            USERS.VERSION,
            USERS.CREATED_AT,
            USERS.UPDATED_AT)
        .from(USERS)
        .fetch(
            r ->
                new UserSummary(
                    r.get(USERS.ID).toString(),
                    r.get(USERS.USER_NAME),
                    r.get(USERS.EMAIL),
                    r.get(USERS.STATUS),
                    r.get(USERS.VERSION),
                    r.get(USERS.CREATED_AT).toInstant(),
                    r.get(USERS.UPDATED_AT).toInstant()));
  }
}
