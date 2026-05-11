package io.github.ppzxc.boilerplate.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spring Context 없이 DSLContext를 직접 사용하는 Adapter 통합 테스트 공통 기반. PostgreSQL Testcontainers Singleton
 * 패턴 (harness.md §7.2). Spring Boot가 필요한 테스트는 {@link AdapterTestBase}를 사용한다.
 */
@Testcontainers
public abstract class PureAdapterTestBase {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("app")
          .withUsername("app")
          .withPassword("app");

  protected String jdbcUrl() {
    return POSTGRES.getJdbcUrl();
  }

  protected String dbUsername() {
    return POSTGRES.getUsername();
  }

  protected String dbPassword() {
    return POSTGRES.getPassword();
  }
}
