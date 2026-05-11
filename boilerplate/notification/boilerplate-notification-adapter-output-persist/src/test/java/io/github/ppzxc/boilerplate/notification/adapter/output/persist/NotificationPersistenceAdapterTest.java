package io.github.ppzxc.boilerplate.notification.adapter.output.persist;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ppzxc.boilerplate.notification.domain.Notification;
import io.github.ppzxc.boilerplate.notification.domain.NotificationChannel;
import io.github.ppzxc.boilerplate.notification.domain.NotificationContent;
import io.github.ppzxc.boilerplate.notification.domain.RecipientUserId;
import io.github.ppzxc.boilerplate.test.PureAdapterTestBase;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

class NotificationPersistenceAdapterTest extends PureAdapterTestBase {

  private NotificationPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    var ds = new PGSimpleDataSource();
    ds.setUrl(jdbcUrl());
    ds.setUser(dbUsername());
    ds.setPassword(dbPassword());

    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()
        .migrate();

    var dsl = DSL.using(ds, SQLDialect.POSTGRES);
    adapter = new NotificationPersistenceAdapter(dsl, event -> {});
  }

  private static Notification createTestNotification() {
    return Notification.create(
        new RecipientUserId(UUID.randomUUID()),
        NotificationChannel.EMAIL,
        new NotificationContent("회원가입을 환영합니다!", "test@example.com 계정으로 가입이 완료되었습니다."),
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void save_and_findById_왕복() {
    var notification = createTestNotification();
    notification.pullDomainEvents(); // append-only: 항상 빈 목록

    adapter.save(notification);

    var loaded = adapter.findById(notification.id()).orElseThrow();
    assertThat(loaded.id()).isEqualTo(notification.id());
    assertThat(loaded.recipientUserId()).isEqualTo(notification.recipientUserId());
    assertThat(loaded.channel()).isEqualTo(NotificationChannel.EMAIL);
    assertThat(loaded.status().name()).isEqualTo("PENDING");
    assertThat(loaded.content().subject()).isEqualTo("회원가입을 환영합니다!");
    // reconstitute()로 복원 — 이벤트 없음 (AD-5)
    assertThat(loaded.pullDomainEvents()).isEmpty();
  }

  @Test
  void save_append_only_도메인이벤트_없음() {
    var notification = createTestNotification();

    // Notification은 append-only — create()에서 이벤트 미발행
    var events = notification.pullDomainEvents();
    assertThat(events).isEmpty();

    adapter.save(notification);

    var loaded = adapter.findById(notification.id()).orElseThrow();
    assertThat(loaded.pullDomainEvents()).isEmpty();
  }

  @Test
  void findById_존재하지않으면_empty() {
    var result =
        adapter.findById(
            new io.github.ppzxc.boilerplate.notification.domain.NotificationId(UUID.randomUUID()));
    assertThat(result).isEmpty();
  }
}
