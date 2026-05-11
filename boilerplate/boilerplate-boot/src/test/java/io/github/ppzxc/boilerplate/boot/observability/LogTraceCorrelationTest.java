package io.github.ppzxc.boilerplate.boot.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = "management.tracing.sampling.probability=1.0")
class LogTraceCorrelationTest {

  private static final String API_VERSION = "2026-05-08";

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

  @Autowired Environment env;

  RestClient restClient;
  ListAppender<ILoggingEvent> listAppender;
  Logger filterLogger;

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> status.isError(), (req, res) -> {})
            .build();

    // RequestIdFilter 로거에 ListAppender 부착 — 요청 처리 스레드에서 직접 로그 발생
    filterLogger =
        (Logger)
            LoggerFactory.getLogger(
                "io.github.ppzxc.boilerplate.identity.adapter.input.api.filter.RequestIdFilter");
    listAppender = new ListAppender<>();
    listAppender.start();
    filterLogger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    filterLogger.detachAppender(listAppender);
  }

  @Test
  void ECS_로깅_형식_설정됨() {
    assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("ecs");
    assertThat(env.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
  }

  @Test
  void 회원가입_요청_시_응답_헤더에_Request_Id_존재() {
    ResponseEntity<Map> response =
        restClient
            .post()
            .uri("/api/identity/users")
            .header("Api-Version", API_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "userName", "트레이스테스트",
                    "email", "trace_" + UUID.randomUUID() + "@test.com",
                    "password", "hashedpw123"))
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    // RequestIdFilter가 MDC에 requestId를 주입하고 응답 헤더에 포함
    assertThat(response.getHeaders().getFirst("Request-Id")).isNotBlank();
  }
}
