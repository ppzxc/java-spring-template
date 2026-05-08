package io.github.ppzxc.boilerplate.identity.adapter.output.persist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ppzxc.boilerplate.identity.application.port.out.OptimisticLockException;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
class UserPersistenceAdapterTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("app")
          .withUsername("app")
          .withPassword("app");

  private UserPersistenceAdapter adapter;

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
    adapter = new UserPersistenceAdapter(dsl, event -> {});
  }

  @Test
  void save_and_findById_왕복() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var user =
        User.create(
            new UserName("홍길동"),
            new Email("test@example.com"),
            new HashedPassword("hashed_pw"),
            now);
    user.pullDomainEvents(); // 이벤트 비움

    adapter.save(user);

    var loaded = adapter.findById(user.id()).orElseThrow();
    assertThat(loaded.email().value()).isEqualTo("test@example.com");
    assertThat(loaded.userName().value()).isEqualTo("홍길동");
    assertThat(loaded.status().name()).isEqualTo("ACTIVE");
    // reconstitute()로 복원 — 이벤트 없음
    assertThat(loaded.pullDomainEvents()).isEmpty();
  }

  @Test
  void save_신규_이벤트_수거() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var user =
        User.create(
            new UserName("이벤트"), new Email("evt@example.com"), new HashedPassword("hashed"), now);

    List<Object> collected = new ArrayList<>();
    var dsl = DSL.using(createDataSource(), SQLDialect.POSTGRES);
    var adapterWithCollector = new UserPersistenceAdapter(dsl, collected::add);

    adapterWithCollector.save(user);

    assertThat(collected).hasSize(1);
  }

  @Test
  void optimistic_lock_충돌() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var user =
        User.create(
            new UserName("락테스트"), new Email("lock@example.com"), new HashedPassword("hashed"), now);
    user.pullDomainEvents();
    adapter.save(user);

    var copy1 = adapter.findById(user.id()).orElseThrow();
    var copy2 = adapter.findById(user.id()).orElseThrow();

    copy1.suspend(now.plusSeconds(1));
    adapter.save(copy1);
    copy1.pullDomainEvents();

    copy2.suspend(now.plusSeconds(2));
    assertThatThrownBy(() -> adapter.save(copy2)).isInstanceOf(OptimisticLockException.class);
  }

  @Test
  void existsByEmail_존재하는경우() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var user =
        User.create(
            new UserName("존재"), new Email("exists@example.com"), new HashedPassword("hashed"), now);
    user.pullDomainEvents();
    adapter.save(user);

    assertThat(adapter.existsByEmail(new Email("exists@example.com"))).isTrue();
    assertThat(adapter.existsByEmail(new Email("notexists@example.com"))).isFalse();
  }

  private PGSimpleDataSource createDataSource() {
    var ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser("app");
    ds.setPassword("app");
    return ds;
  }
}
