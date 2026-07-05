package com.pcverse.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) {
}
