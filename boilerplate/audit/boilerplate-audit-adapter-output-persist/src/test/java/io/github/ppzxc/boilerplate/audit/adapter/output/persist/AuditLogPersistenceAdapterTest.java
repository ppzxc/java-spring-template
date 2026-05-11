package io.github.ppzxc.boilerplate.audit.adapter.output.persist;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ppzxc.boilerplate.audit.domain.AuditEventType;
import io.github.ppzxc.boilerplate.audit.domain.AuditLog;
import io.github.ppzxc.boilerplate.audit.domain.AuditLogId;
import io.github.ppzxc.boilerplate.audit.domain.AuditPayload;
import io.github.ppzxc.boilerplate.audit.domain.AuditedUserId;
import io.github.ppzxc.boilerplate.test.PureAdapterTestBase;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

class AuditLogPersistenceAdapterTest extends PureAdapterTestBase {

  private AuditLogPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    var ds = new PGSimpleDataSource();
    ds.setUrl(jdbcUrl());
    ds.setUser(dbUsername());
    ds.setPassword(dbPassword());

    var flyway =
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
    flyway.clean();
    flyway.migrate();

    var dsl = DSL.using(ds, SQLDialect.POSTGRES);
    adapter = new AuditLogPersistenceAdapter(dsl, event -> {});
  }

  private static AuditLog createTestAuditLog() {
    return AuditLog.create(
        new AuditedUserId(UUID.randomUUID()),
        AuditEventType.USER_REGISTERED,
        new AuditPayload("{\"userName\":\"홍길동\",\"email\":\"test@example.com\"}"),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:01Z"));
  }

  @Test
  void save_and_findById_왕복() {
    var log = createTestAuditLog();

    adapter.save(log);

    var loaded = adapter.findById(log.id()).orElseThrow();
    assertThat(loaded.id()).isEqualTo(log.id());
    assertThat(loaded.subjectUserId()).isEqualTo(log.subjectUserId());
    assertThat(loaded.eventType()).isEqualTo(AuditEventType.USER_REGISTERED);
    // JSONB는 키 순서를 보장하지 않으므로 contains로 검증
    assertThat(loaded.payload().value()).contains("홍길동").contains("test@example.com");
    assertThat(loaded.occurredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(loaded.recordedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:01Z"));
    assertThat(loaded.version()).isEqualTo(0L);
    // reconstitute()로 복원 — 이벤트 없음 (AD-5)
    assertThat(loaded.pullDomainEvents()).isEmpty();
  }

  @Test
  void save_payload_jsonb_왕복_escape_문자() {
    var escapedPayload = "{\"userName\":\"Bob \\\"Builder\\\"\",\"email\":\"bob@test.com\"}";
    var log =
        AuditLog.create(
            new AuditedUserId(UUID.randomUUID()),
            AuditEventType.USER_REGISTERED,
            new AuditPayload(escapedPayload),
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:01Z"));

    adapter.save(log);

    var loaded = adapter.findById(log.id()).orElseThrow();
    assertThat(loaded.payload().value()).contains("Bob");
    assertThat(loaded.payload().value()).contains("Builder");
  }

  @Test
  void findById_존재하지않으면_empty() {
    var result = adapter.findById(new AuditLogId(UUID.randomUUID()));
    assertThat(result).isEmpty();
  }

  @Test
  void save_append_only_도메인이벤트_없음() {
    var log = createTestAuditLog();
    assertThat(log.pullDomainEvents()).isEmpty();

    adapter.save(log);

    var loaded = adapter.findById(log.id()).orElseThrow();
    assertThat(loaded.pullDomainEvents()).isEmpty();
  }
}
