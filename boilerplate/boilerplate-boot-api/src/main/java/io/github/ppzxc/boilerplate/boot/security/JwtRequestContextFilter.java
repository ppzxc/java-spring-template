package io.github.ppzxc.boilerplate.boot.security;

import io.github.ppzxc.boilerplate.shared.Permission;
import io.github.ppzxc.boilerplate.shared.RequestContext;
import io.github.ppzxc.boilerplate.shared.RequestScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class JwtRequestContextFilter extends OncePerRequestFilter {

  private static final String TID_CLAIM = "tid";
  private static final String SCOPE_CLAIM = "scope";

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      var jwt = jwtAuth.getToken();
      var ctx =
          new RequestContext(
              UUID.fromString(jwt.getSubject()),
              jwt.getClaimAsString(TID_CLAIM) != null ? jwt.getClaimAsString(TID_CLAIM) : "default",
              parsePermissions(jwt.getClaimAsString(SCOPE_CLAIM)));
      try {
        ScopedValue.where(RequestScope.CTX, ctx)
            .call(
                () -> {
                  chain.doFilter(req, res);
                  return null;
                });
      } catch (RuntimeException | IOException | ServletException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    } else {
      chain.doFilter(req, res);
    }
  }

  private static Set<Permission> parsePermissions(String scope) {
    if (scope == null || scope.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(scope.split(" ", -1))
        .filter(s -> s.matches("[a-z]+:[a-z]+"))
        .map(Permission::new)
        .collect(Collectors.toUnmodifiableSet());
  }
}
