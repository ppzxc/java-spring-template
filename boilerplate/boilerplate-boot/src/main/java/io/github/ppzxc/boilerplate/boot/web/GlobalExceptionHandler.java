package io.github.ppzxc.boilerplate.boot.web;

import io.github.ppzxc.boilerplate.shared.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 9457 problem+json 전역 예외 변환 핸들러 (adapter.md §8). BC별 예외는 각 BC의 adapter-input-api에 위치. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ProblemDetail handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.NOT_FOUND, "Not Found", "NOT_FOUND", msg(ex, "Resource not found"), req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    var violations =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error -> {
                  var field =
                      error instanceof FieldError fe ? fe.getField() : error.getObjectName();
                  var desc = Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value");
                  return new FieldViolation(field, desc, "VALIDATION_FAILED");
                })
            .toList();
    var p =
        problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable Entity",
            "VALIDATION_FAILED",
            "Request validation failed",
            req);
    p.setProperty("details", violations);
    return p;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
    var p =
        problem(
            HttpStatus.FORBIDDEN,
            "Permission Denied",
            "PERMISSION_DENIED",
            msg(ex, "Access denied"),
            req);
    p.setProperty("requiredPermission", ex.requiredPermission());
    return p;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        "INVALID_ARGUMENT",
        msg(ex, "Invalid argument"),
        req);
  }

  private ProblemDetail problem(
      HttpStatus status, String title, String code, String detail, HttpServletRequest req) {
    var p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setTitle(title);
    p.setType(URI.create("about:blank"));
    p.setInstance(URI.create(req.getRequestURI()));
    p.setProperty("code", code);
    var traceId = MDC.get("traceId");
    if (traceId == null) {
      traceId = MDC.get("requestId");
    }
    if (traceId != null) {
      p.setProperty("traceId", traceId);
    }
    return p;
  }

  private static String msg(Exception ex, String fallback) {
    return Objects.requireNonNullElse(ex.getMessage(), fallback);
  }
}
