package io.github.ppzxc.boilerplate.identity.adapter.input.api;

import io.github.ppzxc.boilerplate.identity.application.port.output.OptimisticLockException;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
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

/**
 * Identity BC 예외 → RFC 9457 변환 핸들러 (adapter.md §8). boot 모듈이 identity 내부 타입을 직접 참조하지 않도록
 * adapter-input-api에 위치.
 */
@RestControllerAdvice
class IdentityExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  ProblemDetail handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {
    return problem(HttpStatus.NOT_FOUND, "Not Found", "NOT_FOUND", "Resource not found", req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
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

  @ExceptionHandler(UserException.InvalidCredentialException.class)
  ProblemDetail handleInvalidCredential(
      UserException.InvalidCredentialException ex, HttpServletRequest req) {
    return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_CREDENTIAL", msg(ex), req);
  }

  @ExceptionHandler(UserException.IneligibleStatusException.class)
  ProblemDetail handleIneligibleStatus(
      UserException.IneligibleStatusException ex, HttpServletRequest req) {
    return problem(HttpStatus.FORBIDDEN, "Forbidden", "USER_STATUS_INELIGIBLE", msg(ex), req);
  }

  @ExceptionHandler(UserException.NotFoundException.class)
  ProblemDetail handleNotFound(UserException.NotFoundException ex, HttpServletRequest req) {
    return problem(HttpStatus.NOT_FOUND, "User Not Found", "USER_NOT_FOUND", msg(ex), req);
  }

  @ExceptionHandler(UserException.AlreadyExistsException.class)
  ProblemDetail handleAlreadyExists(
      UserException.AlreadyExistsException ex, HttpServletRequest req) {
    return problem(HttpStatus.CONFLICT, "Conflict", "USER_ALREADY_EXISTS", msg(ex), req);
  }

  @ExceptionHandler(UserException.AlreadySuspendedException.class)
  ProblemDetail handleAlreadySuspended(
      UserException.AlreadySuspendedException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Unprocessable Entity",
        "USER_ALREADY_SUSPENDED",
        msg(ex),
        req);
  }

  @ExceptionHandler(UserException.AlreadyDeactivatedException.class)
  ProblemDetail handleAlreadyDeactivated(
      UserException.AlreadyDeactivatedException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Unprocessable Entity",
        "USER_ALREADY_DEACTIVATED",
        msg(ex),
        req);
  }

  @ExceptionHandler(OptimisticLockException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.PRECONDITION_FAILED,
        "Precondition Failed",
        "ETAG_MISMATCH",
        "ETag mismatch: resource was modified concurrently",
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

  private static String msg(Exception ex) {
    return Objects.requireNonNullElse(ex.getMessage(), "Unknown error");
  }

  record FieldViolation(String field, String description, String reason) {}
}
