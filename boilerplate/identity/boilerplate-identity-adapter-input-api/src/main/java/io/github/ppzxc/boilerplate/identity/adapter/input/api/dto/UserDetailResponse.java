package io.github.ppzxc.boilerplate.identity.adapter.input.api.dto;

import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserResult;
import io.github.ppzxc.boilerplate.identity.application.dto.UserSummary;
import java.time.Instant;

/** 사용자 상세 응답 DTO (AIP-203 OUTPUT_ONLY 필드 포함). */
public record UserDetailResponse(
    String id,
    String userName,
    String email,
    UserState state,
    String etag,
    Instant createdAt,
    Instant updatedAt) {

  public static UserDetailResponse from(RegisterUserResult result) {
    return new UserDetailResponse(
        result.userId(),
        result.userName(),
        result.email(),
        UserState.fromStatus(result.status()),
        "v" + result.version(),
        result.createdAt(),
        result.updatedAt());
  }

  public static UserDetailResponse from(UserSummary summary) {
    return new UserDetailResponse(
        summary.userId(),
        summary.userName(),
        summary.email(),
        UserState.fromStatus(summary.status()),
        "v" + summary.version(),
        summary.createdAt(),
        summary.updatedAt());
  }
}
