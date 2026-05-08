package io.github.ppzxc.boilerplate.identity.adapter.input.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 회원가입 요청 DTO. */
public record UserRegistrationRequest(
    @NotBlank(message = "userName must not be blank") String userName,
    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid")
        String email,
    @NotBlank(message = "password must not be blank") String password) {}
