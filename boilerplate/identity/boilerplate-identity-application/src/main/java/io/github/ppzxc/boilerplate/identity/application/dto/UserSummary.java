package io.github.ppzxc.boilerplate.identity.application.dto;

import java.time.Instant;

/** 사용자 조회 결과 DTO (Query Side). */
public record UserSummary(
    String userId,
    String userName,
    String email,
    String status,
    long version,
    Instant createdAt,
    Instant updatedAt) {}
