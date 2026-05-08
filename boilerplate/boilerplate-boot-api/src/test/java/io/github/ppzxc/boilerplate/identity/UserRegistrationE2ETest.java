package io.github.ppzxc.boilerplate.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Identity BC E2E 테스트 — 핵심 경로만 (testing.md §9). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserRegistrationE2ETest {

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

  RestClient restClient;

  private static final String API_PATH = "/api/identity/users";
  private static final String API_VERSION = "2026-05-08";

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(
                status -> status.isError(), // 4xx/5xx 수신 시 예외 없이 ResponseEntity 반환
                (req, res) -> {})
            .build();
  }

  @Test
  void 회원가입_성공_201() {
    var body =
        Map.of("userName", "홍길동", "email", "e2e_register@test.com", "password", "hashedpw123");

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(API_PATH)
            .header("Api-Version", API_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    URI location = response.getHeaders().getLocation();
    assertThat(location).isNotNull();
    assertThat(location.toString()).contains("/api/identity/users/");
    assertThat(response.getHeaders().getFirst("Request-Id")).isNotBlank();

    Map<?, ?> responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.get("id")).isNotNull();
    assertThat(responseBody.get("state")).isEqualTo("ACTIVE");
    assertThat(responseBody.get("etag")).isEqualTo("v0");
  }

  @Test
  void 중복_이메일_409() {
    var body = Map.of("userName", "중복유저", "email", "e2e_dup@test.com", "password", "hashedpw123");

    restClient
        .post()
        .uri(API_PATH)
        .header("Api-Version", API_VERSION)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toEntity(Map.class);

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(API_PATH)
            .header("Api-Version", API_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");

    Map<?, ?> responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.get("code")).isEqualTo("USER_ALREADY_EXISTS");
    assertThat(responseBody.get("status")).isEqualTo(409);
  }

  @Test
  void Api_Version_누락_400() {
    var body = Map.of("userName", "테스트", "email", "e2e_no_version@test.com", "password", "pw");

    ResponseEntity<Map> response =
        restClient
            .post()
            .uri(API_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");

    Map<?, ?> responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.get("code")).isEqualTo("API_VERSION_REQUIRED");
  }
}
