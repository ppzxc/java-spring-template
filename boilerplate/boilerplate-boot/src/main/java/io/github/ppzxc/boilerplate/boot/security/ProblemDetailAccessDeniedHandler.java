package io.github.ppzxc.boilerplate.boot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
      throws IOException {
    ProblemDetailAuthEntryPoint.write(
        res,
        403,
        "PERMISSION_DENIED",
        Objects.requireNonNullElse(ex.getMessage(), "Access denied"),
        req.getRequestURI());
  }
}
