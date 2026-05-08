package io.github.ppzxc.boilerplate.identity.application.dto;

import java.time.Instant;

/** 사용자 등록 결과 DTO (Command Side). */
public record RegisterUserResult(
    String userId,
    String userName,
    String email,
    String status,
    long version,
    Instant createdAt,
    Instant updatedAt) {}
