package io.github.ppzxc.boilerplate.identity.adapter.input.api.dto;

/** RFC 9457 problem+json 의 details[] 항목 (google.rpc.BadRequest 대응). */
public record FieldViolation(String field, String description, String reason) {}
