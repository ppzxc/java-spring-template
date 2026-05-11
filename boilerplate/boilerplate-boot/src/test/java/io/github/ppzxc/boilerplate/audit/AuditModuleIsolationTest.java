package io.github.ppzxc.boilerplate.audit;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Audit BC 단독 기동 격리 테스트 (testing.md §8). */
@ApplicationModuleTest(mode = BootstrapMode.STANDALONE)
class AuditModuleIsolationTest {

  static final PostgreSQLContainer<?> POSTGRES;

  static {
    POSTGRES =
        new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("app");
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void audit_모듈_단독_기동() {
    // Spring Modulith STANDALONE 모드로 audit 모듈만 기동 — 컨텍스트 로딩 자체가 검증
  }
}
