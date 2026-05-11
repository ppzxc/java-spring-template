package io.github.ppzxc.boilerplate.boot.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityFilterChainE2ETest {

  private static final String HMAC_SECRET = "dev-secret-change-me-in-production-min-256-bits";
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

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient =
        RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> status.isError(), (req, res) -> {})
            .build();
  }

  @Test
  void 토큰_미첨부_인증필요_엔드포인트_401() {
    var response =
        restClient
            .get()
            .uri("/api/identity/users/" + UUID.randomUUID())
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
  }

  @Test
  void 회원가입_토큰_없어도_permitAll_보안차단없음() {
    var body =
        Map.of(
            "userName", "보안테스트",
            "email", "security_e2e_" + UUID.randomUUID() + "@test.com",
            "password", "hashedpw123");

    var response =
        restClient
            .post()
            .uri("/api/identity/users")
            .header("Api-Version", API_VERSION)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void 만료된_토큰_401() throws Exception {
    var token = createExpiredToken();

    var response =
        restClient
            .get()
            .uri("/api/identity/users/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void actuator_health_readiness_200() {
    var response =
        restClient.get().uri("/actuator/health/readiness").retrieve().toEntity(Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private static String createExpiredToken() throws Exception {
    var signer = new MACSigner(HMAC_SECRET.getBytes(StandardCharsets.UTF_8));
    var claims =
        new JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .claim("tid", "test")
            .claim("scope", "")
            .issueTime(new Date(System.currentTimeMillis() - 7200_000))
            .expirationTime(new Date(System.currentTimeMillis() - 3600_000))
            .build();
    var signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    signedJWT.sign(signer);
    return signedJWT.serialize();
  }
}
