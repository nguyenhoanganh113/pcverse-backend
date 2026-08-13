package com.pcverse.dto.request;

import jakarta.validation.constraints.Size;

public record AdminBrandSearchRequest(

        @Size(
                max = 120,
                message = "Keyword must not exceed 120 characters"
        )
        String keyword,

        Boolean active

) {
    public AdminBrandSearchRequest {
        keyword = stripToNull(keyword);
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
