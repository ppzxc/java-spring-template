package io.github.ppzxc.boilerplate.boot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.ppzxc.boilerplate.shared.Permission;
import io.github.ppzxc.boilerplate.shared.RequestScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtRequestContextFilterTest {

  private final JwtRequestContextFilter filter = new JwtRequestContextFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void jwt인증_없으면_체인만_통과() throws Exception {
    var req = mock(HttpServletRequest.class);
    var res = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);

    filter.doFilterInternal(req, res, chain);

    verify(chain).doFilter(req, res);
  }

  @Test
  void jwt_sub가_userId로_변환() throws Exception {
    var userId = UUID.randomUUID();
    setJwtAuth(userId.toString(), "tenant1", "");

    var captured = new AtomicReference<UUID>();
    var req = mock(HttpServletRequest.class);
    var res = mock(HttpServletResponse.class);
    FilterChain chain = (r, s) -> captured.set(RequestScope.CTX.get().userId());

    filter.doFilterInternal(req, res, chain);

    assertThat(captured.get()).isEqualTo(userId);
  }

  @Test
  void tid_클레임_없으면_default() throws Exception {
    var jwt =
        Jwt.withTokenValue("test-token")
            .subject(UUID.randomUUID().toString())
            .claim("scope", "")
            .header("alg", "HS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build(); // tid 클레임 의도적으로 누락
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

    var captured = new AtomicReference<String>();
    var req = mock(HttpServletRequest.class);
    var res = mock(HttpServletResponse.class);
    FilterChain chain = (r, s) -> captured.set(RequestScope.CTX.get().tenantId());

    filter.doFilterInternal(req, res, chain);

    assertThat(captured.get()).isEqualTo("default");
  }

  @Test
  void scope_파싱_permissions_변환() throws Exception {
    setJwtAuth(UUID.randomUUID().toString(), "t1", "user:read user:write");

    var captured = new AtomicReference<Set<Permission>>();
    var req = mock(HttpServletRequest.class);
    var res = mock(HttpServletResponse.class);
    FilterChain chain = (r, s) -> captured.set(RequestScope.CTX.get().permissions());

    filter.doFilterInternal(req, res, chain);

    assertThat(captured.get())
        .containsExactlyInAnyOrder(new Permission("user:read"), new Permission("user:write"));
  }

  @Test
  void 빈_scope는_빈_permissions() throws Exception {
    setJwtAuth(UUID.randomUUID().toString(), "t1", "   ");

    var captured = new AtomicReference<Set<Permission>>();
    var req = mock(HttpServletRequest.class);
    var res = mock(HttpServletResponse.class);
    FilterChain chain = (r, s) -> captured.set(RequestScope.CTX.get().permissions());

    filter.doFilterInternal(req, res, chain);

    assertThat(captured.get()).isEmpty();
  }

  private void setJwtAuth(String subject, String tid, String scope) {
    var builder =
        Jwt.withTokenValue("test-token")
            .subject(subject)
            .claim("scope", scope)
            .header("alg", "HS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600));
    if (tid != null) {
      builder.claim("tid", tid);
    }
    var jwt = builder.build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
