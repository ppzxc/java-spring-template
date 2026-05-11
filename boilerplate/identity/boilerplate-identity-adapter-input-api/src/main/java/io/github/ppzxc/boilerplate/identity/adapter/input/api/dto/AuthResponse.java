package io.github.ppzxc.boilerplate.identity.adapter.input.api.dto;

/** Auth Response DTO. */
public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
