package io.github.ppzxc.boilerplate.boot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

class ProblemDetailAuthEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
      throws IOException {
    write(res, 401, "AUTHENTICATION_REQUIRED", "Bearer token is required", req.getRequestURI());
  }

  static void write(
      HttpServletResponse res, int status, String code, String detail, String instance)
      throws IOException {
    res.setStatus(status);
    res.setContentType("application/problem+json");
    var body =
        """
            {"type":"about:blank","title":"%s","status":%d,\
            "code":"%s","detail":"%s","instance":"%s"}"""
            .formatted(HttpStatus.valueOf(status).getReasonPhrase(), status, code, detail, instance)
            .getBytes(StandardCharsets.UTF_8);
    res.setContentLength(body.length);
    res.getOutputStream().write(body);
  }
}
