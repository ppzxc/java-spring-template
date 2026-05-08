package io.github.ppzxc.boilerplate.identity.adapter.output.persist;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class UserQueryAdapterTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("app")
          .withUsername("app")
          .withPassword("app");

  private UserQueryAdapter queryAdapter;
  private UserPersistenceAdapter persistAdapter;

  @BeforeEach
  void setUp() {
    var ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser("app");
    ds.setPassword("app");

    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()
        .migrate();

    var dsl = DSL.using(ds, SQLDialect.POSTGRES);
    queryAdapter = new UserQueryAdapter(dsl);
    persistAdapter = new UserPersistenceAdapter(dsl, event -> {});
  }

  @Test
  void findSummaryById_존재하는사용자() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var user =
        User.create(
            new UserName("요약테스트"),
            new Email("summary@example.com"),
            new HashedPassword("hashed"),
            now);
    user.pullDomainEvents();
    persistAdapter.save(user);

    var result = queryAdapter.findSummaryById(user.id().value().toString());
    assertThat(result).isPresent();
    assertThat(result.get().userName()).isEqualTo("요약테스트");
    assertThat(result.get().email()).isEqualTo("summary@example.com");
    assertThat(result.get().status()).isEqualTo("ACTIVE");
    assertThat(result.get().version()).isEqualTo(0L);
  }

  @Test
  void findSummaryById_존재하지않는사용자() {
    var result = queryAdapter.findSummaryById("00000000-0000-7000-8000-000000000000");
    assertThat(result).isEmpty();
  }

  @Test
  void findAll_여러사용자() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    for (int i = 1; i <= 3; i++) {
      var user =
          User.create(
              new UserName("사용자" + i),
              new Email("user" + i + "@example.com"),
              new HashedPassword("hashed"),
              now);
      user.pullDomainEvents();
      persistAdapter.save(user);
    }

    var all = queryAdapter.findAll();
    assertThat(all).hasSizeGreaterThanOrEqualTo(3);
  }
}
