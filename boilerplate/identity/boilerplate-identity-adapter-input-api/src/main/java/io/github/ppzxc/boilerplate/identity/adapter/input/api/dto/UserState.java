package io.github.ppzxc.boilerplate.identity.adapter.input.api.dto;

/** API 응답용 사용자 상태 (AIP-216 — state 필드). */
public enum UserState {
  STATE_UNSPECIFIED,
  ACTIVE,
  SUSPENDED,
  DEACTIVATED;

  public static UserState fromStatus(String status) {
    return switch (status) {
      case "ACTIVE" -> ACTIVE;
      case "SUSPENDED" -> SUSPENDED;
      case "DEACTIVATED" -> DEACTIVATED;
      default -> STATE_UNSPECIFIED;
    };
  }
}
