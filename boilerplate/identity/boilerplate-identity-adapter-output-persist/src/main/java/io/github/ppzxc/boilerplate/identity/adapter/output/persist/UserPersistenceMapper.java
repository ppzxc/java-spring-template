package io.github.ppzxc.boilerplate.identity.adapter.output.persist;

import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserId;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import io.github.ppzxc.boilerplate.identity.domain.model.UserStatus;
import io.github.ppzxc.boilerplate.identity.persistence.jooq.tables.records.UsersRecord;

/** Domain ↔ jOOQ Record 변환 매퍼 (AD-4). */
public final class UserPersistenceMapper {

  private UserPersistenceMapper() {}

  public static User toDomain(UsersRecord record) {
    return User.reconstitute(
        new UserId(record.getId()),
        new Email(record.getEmail()),
        new UserName(record.getUserName()),
        new HashedPassword(record.getHashedPassword()),
        record.getCredCreatedAt().toInstant(),
        UserStatus.valueOf(record.getStatus()),
        record.getCreatedAt().toInstant(),
        record.getUpdatedAt().toInstant(),
        record.getVersion());
  }
}
