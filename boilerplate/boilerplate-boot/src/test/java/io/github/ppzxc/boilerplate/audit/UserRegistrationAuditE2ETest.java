package io.github.ppzxc.boilerplate.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Identity 회원가입 → Audit BC 감사 로그 적재 E2E. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserRegistrationAuditE2ETest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("app")
          .withUsername("app")
          .withPassword("app");

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbcTemplate;

  RestClient restClient;

  private static final String REGISTER_PATH = "/api/identity/users";
  private static final String API_VERSION = "2026-05-08";

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> status.isError(), (req, res) -> {})
            .build();
  }

  @Test
  void 회원가입_후_audit_log_테이블에_1건_적재() {
    var body =
        Map.of(
            "userName", "감사테스트",
            "email", "audit_e2e@test.com",
            "password", "hashedpw123");

    var response =
        restClient
            .post()
            .uri(REGISTER_PATH)
            .header("Api-Version", API_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    var responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    var userId = UUID.fromString(responseBody.get("id").toString());

    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              var count =
                  jdbcTemplate.queryForObject(
                      "SELECT COUNT(*) FROM audit_log WHERE subject_user_id = ?",
                      Integer.class,
                      userId);
              assertThat(count).isEqualTo(1);
            });

    var row =
        jdbcTemplate.queryForMap(
            "SELECT event_type, payload::text FROM audit_log WHERE subject_user_id = ?", userId);
    assertThat(row.get("event_type")).isEqualTo("USER_REGISTERED");
    assertThat(row.get("payload").toString()).contains("감사테스트").contains("audit_e2e@test.com");
  }
}
