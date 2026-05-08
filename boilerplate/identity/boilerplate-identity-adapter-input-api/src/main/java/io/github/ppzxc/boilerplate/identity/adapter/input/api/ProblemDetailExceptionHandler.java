package io.github.ppzxc.boilerplate.identity.adapter.input.api;

import io.github.ppzxc.boilerplate.identity.adapter.input.api.dto.FieldViolation;
import io.github.ppzxc.boilerplate.identity.application.port.out.OptimisticLockException;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.shared.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 9457 problem+json 예외 변환 핸들러 (AIP-193). */
@RestControllerAdvice
public class ProblemDetailExceptionHandler {

  @ExceptionHandler(UserException.NotFoundException.class)
  public ProblemDetail handleNotFound(UserException.NotFoundException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.NOT_FOUND, "User Not Found", "USER_NOT_FOUND", msg(ex, "User not found"), req);
  }

  @ExceptionHandler(UserException.AlreadyExistsException.class)
  public ProblemDetail handleAlreadyExists(
      UserException.AlreadyExistsException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.CONFLICT,
        "Conflict",
        "USER_ALREADY_EXISTS",
        msg(ex, "User already exists"),
        req);
  }

  @ExceptionHandler(UserException.AlreadySuspendedException.class)
  public ProblemDetail handleAlreadySuspended(
      UserException.AlreadySuspendedException ex, HttpServletRequest req) {
    var p =
        problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable Entity",
            "USER_ALREADY_SUSPENDED",
            msg(ex, "User is already suspended"),
            req);
    p.setProperty(
        "details",
        List.of(new FieldViolation("status", "User is already suspended", "ALREADY_SUSPENDED")));
    return p;
  }

  @ExceptionHandler(UserException.AlreadyDeactivatedException.class)
  public ProblemDetail handleAlreadyDeactivated(
      UserException.AlreadyDeactivatedException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Unprocessable Entity",
        "USER_ALREADY_DEACTIVATED",
        msg(ex, "User is already deactivated"),
        req);
  }

  @ExceptionHandler(OptimisticLockException.class)
  public ProblemDetail handleOptimisticLock(OptimisticLockException ex, HttpServletRequest req) {
    return problem(
        HttpStatus.PRECONDITION_FAILED,
        "Precondition Failed",
        "ETAG_MISMATCH",
        "ETag mismatch: resource was modified concurrently",
        req);
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
