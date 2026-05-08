package io.github.ppzxc.boilerplate.identity.adapter.input.api.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Api-Version 헤더 필수 검증 필터 (RESTful Standard T1). */
@Component
@Order(1)
public class ApiVersionFilter implements Filter {

  private static final String API_VERSION_HEADER = "Api-Version";
  private static final String PROBLEM_JSON = "application/problem+json";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var httpRequest = (HttpServletRequest) request;
    var httpResponse = (HttpServletResponse) response;

    var requestUri = httpRequest.getRequestURI();
    if (requestUri.startsWith("/api/") && httpRequest.getHeader(API_VERSION_HEADER) == null) {
      httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      httpResponse.setContentType(PROBLEM_JSON);
      var body =
          ("""
              {"type":"about:blank","title":"Bad Request","status":400,\
              "code":"API_VERSION_REQUIRED",\
              "detail":"Api-Version header is required",\
              "instance":"%s"}""")
              .formatted(requestUri)
              .getBytes(StandardCharsets.UTF_8);
      httpResponse.setContentLength(body.length);
      httpResponse.getOutputStream().write(body);
      return;
    }

    chain.doFilter(request, response);
  }
}
