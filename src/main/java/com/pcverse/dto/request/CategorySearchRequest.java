package com.pcverse.dto.request;

import jakarta.validation.constraints.Size;

public record CategorySearchRequest(
        @Size(max = 150)
        String keyword
) {
    public CategorySearchRequest {
        keyword = stripToNull(keyword);
    }
    private static String stripToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
